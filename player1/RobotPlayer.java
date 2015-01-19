package player1;
import java.util.Random;
import java.lang.Math.*;

import battlecode.common.*;

public class RobotPlayer{
	static Direction facing;
	static Random rand;
	static RobotController rc;
	
	public static void run(RobotController myrc){
		rc=myrc;
		rand = new Random(rc.getID());
		facing = getRandDir();  //random starting direction
		while(true){
			try {
				if(rc.getType()==RobotType.HQ){
					attackEnemyZero();
					spawnUnit(RobotType.BEAVER);
				} else if(rc.getType()==RobotType.BEAVER){
					attackEnemyZero();
					if(Clock.getRoundNum()<700){
						buildUnit(RobotType.MINERFACTORY);
					} else{
						buildUnit(RobotType.BARRACKS);
					}
					mineAndMove();
				} else if(rc.getType()==RobotType.MINER){
					attackEnemyZero();
					mineAndMove();
				} else if(rc.getType()==RobotType.MINERFACTORY){
					spawnUnit(RobotType.MINER);
				} else if(rc.getType()==RobotType.BARRACKS){
					spawnUnit(RobotType.SOLDIER);
				} else if(rc.getType()==RobotType.TOWER){
					attackEnemyZero();
				} else if(rc.getType()==RobotType.SOLDIER){
					attackEnemyZero();
					moveRandomly();
				}
				tranferSupplies();
				
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			
			rc.yield();
		}
		
	}

	private static void tranferSupplies() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transfer = 0;
		MapLocation transferTo = null;
		for(RobotInfo ri: nearbyAllies){
			if(ri.supplyLevel<lowestSupply){
				lowestSupply=ri.supplyLevel;
				transfer=(rc.getSupplyLevel()-ri.supplyLevel)/2;
				transferTo = ri.location;
			}
		}
		if(transferTo != null){
			rc.transferSupplies((int)transfer, transferTo);
		}
		
	}

	private static void buildUnit(RobotType type) throws GameActionException {
		if(rc.getTeamOre()>type.oreCost){
			Direction buildDir = getRandDir();
			if(rc.isCoreReady()&&rc.canBuild(buildDir, type)){
				rc.build(buildDir,type);
			}
		}
		
	}

	private static void attackEnemyZero() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),rc.getType().attackRadiusSquared,rc.getTeam().opponent());
		if(nearbyEnemies.length>0){
			if(rc.isWeaponReady()&& rc.canAttackLocation(nearbyEnemies[0].location) ){
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
		
	}

	private static void spawnUnit(RobotType type) throws GameActionException {
		Direction randDir = getRandDir();
		if(rc.isCoreReady()&&rc.canSpawn(randDir, type)){
			rc.spawn(randDir, type); 
		} 
	}

	private static Direction getRandDir() {
		return Direction.values()[(int)(rand.nextDouble()*8)]; 
	}

	private static void mineAndMove() throws GameActionException {
		if(rc.senseOre(rc.getLocation())>2){
			if(rc.isCoreReady()&&rc.canMine()){  //mine ore if there is some
				rc.mine(); 
			}
		} else {  // there is no ore :(
			moveRandomly();
		}
		
	}

	private static void moveRandomly() throws GameActionException {
		if(rand.nextDouble()<0.05){
			if(rand.nextDouble()<.5){
				facing=facing.rotateLeft();
			} else {
				facing= facing.rotateRight();
			}
		}
		MapLocation tileInFront = rc.getLocation().add(facing);
		
		//Check for towers
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		boolean safe = true;
		for(MapLocation m: enemyTowers){
			if(m.distanceSquaredTo(tileInFront)<=RobotType.TOWER.attackRadiusSquared){
				safe=false;
				break;
			}
		}
		
		//check that move is safe and on map
		if(rc.senseTerrainTile(tileInFront) != TerrainTile.NORMAL || !safe){
			facing = facing.rotateLeft();
		} else {
			if(rc.isCoreReady()&&rc.canMove(facing)){
				rc.move(facing);
			}
		}
		
		if(rc.isCoreReady() && rc.canMove(facing)){
			rc.move(facing);
		}
		
	}
}