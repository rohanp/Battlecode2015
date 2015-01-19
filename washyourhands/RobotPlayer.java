package washyourhands;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	static int maxBeavers;
	static int maxMiners;
	static Direction facing;
	static Random rand;
	static RobotController rc;
	
	public static void run(RobotController myrc) {
        BaseBot myself;
		rc=myrc;
		rand = new Random(rc.getID());
		facing = getRandDir(); 
        if (rc.getType() == RobotType.HQ) {
            myself = new HQ(rc);
        } else if (rc.getType() == RobotType.BEAVER) {
            myself = new Beaver(rc);
        } else if (rc.getType() == RobotType.BARRACKS) {
            myself = new Barracks(rc);
        } else if (rc.getType() == RobotType.SOLDIER) {
            myself = new Soldier(rc);
        } else if (rc.getType() == RobotType.TOWER) {
            myself = new Tower(rc);
        } else {
            myself = new BaseBot(rc);
        }

        while (true) {
            try {
                myself.go();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	public static Direction getRandDir() {
		return Direction.values()[(int)(rand.nextDouble()*8)]; 
	}

    public static class BaseBot {
        protected RobotController rc;
        protected MapLocation myHQ, theirHQ;
        protected Team myTeam, theirTeam;

        public BaseBot(RobotController rc) {
            this.rc = rc;
            this.myHQ = rc.senseHQLocation();
            this.theirHQ = rc.senseEnemyHQLocation();
            this.myTeam = rc.getTeam();
            this.theirTeam = this.myTeam.opponent();
        }
        
	    public void moveRandomly() throws GameActionException {
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
    	public Direction getRandDir() {
    		return Direction.values()[(int)(rand.nextDouble()*8)]; 
    	}
        
    	public void spawnUnit(RobotType type, Direction dir) throws GameActionException {
    		if(rc.isCoreReady()&&rc.canSpawn(dir, type)){
    			rc.spawn(dir, type); 
    		} 
    	}
    	public void buildUnit(RobotType type, Direction buildDir) throws GameActionException {
    		if(rc.getTeamOre()>type.oreCost){
    			if(rc.isCoreReady()&&rc.canBuild(buildDir, type)){
    				rc.build(buildDir,type);
    			}
    		}
    		
    	}

        public Direction[] getDirectionsToward(MapLocation dest) {
            Direction toDest = rc.getLocation().directionTo(dest);
            Direction[] dirs = {toDest,
		    		toDest.rotateLeft(), toDest.rotateRight(),
				toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

            return dirs;
        }

        public Direction getMoveDir(MapLocation dest) {
            Direction[] dirs = getDirectionsToward(dest);
            for (Direction d : dirs) {
                if (rc.canMove(d)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getSpawnDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canSpawn(d, type)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getBuildDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canBuild(d, type)) {
                    return d;
                }
            }
            return null;
        }
        
    	public void tranferSupplies() throws GameActionException {
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
        
        public MapLocation getBuildLocation(RobotType type) throws GameActionException {
            MapLocation loc = rc.senseHQLocation();
            int r =1;
            while(true){ //keep expanding out from the HQ till you find a valid location
            	MapLocation[] nearLocs=MapLocation.getAllMapLocationsWithinRadiusSq(loc, r);
            	for(MapLocation l : nearLocs){
            		if(isValidBuildLoc(l)){
            			return l;
            		}
            	}
            	r+=1;
            }
        }

        private boolean isValidBuildLoc(MapLocation loc) throws GameActionException {
        	Direction[] directions = {Direction.NORTH,  Direction.EAST, Direction.SOUTH, Direction.WEST};
			for(Direction dir: directions){
				if(rc.isLocationOccupied(loc.add(dir))){
					return false;
				}
			}
			return true;
		}

		public RobotInfo[] getAllies() {
            RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
            return allies;
        }

        public RobotInfo[] getEnemiesInAttackingRange() {
            RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, theirTeam);
            return enemies;
        }

        public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
            if (enemies.length == 0) {
                return;
            }

            double minEnergon = Double.MAX_VALUE;
            MapLocation toAttack = null;
            for (RobotInfo info : enemies) {
                if (info.health < minEnergon) {
                    toAttack = info.location;
                    minEnergon = info.health;
                }
            }

            rc.attackLocation(toAttack);
        }

        public void beginningOfTurn() {
            if (rc.senseEnemyHQLocation() != null) {
                this.theirHQ = rc.senseEnemyHQLocation();
            }
        }

        public void endOfTurn() {
        }

        public void go() throws GameActionException {
            beginningOfTurn();
            execute();
            endOfTurn();
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class HQ extends BaseBot {
        public HQ(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	if(rc.readBroadcast(2)<=3){
        		spawnUnit(RobotType.BEAVER, getSpawnDirection(RobotType.BEAVER));
        		rc.broadcast(2, rc.readBroadcast(2) + 1 );
        	}
            rc.yield();
        }
    }

    public static class Beaver extends BaseBot {
        public Beaver(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	/*if((rc.readBroadcast(3)<=1 && rc.getTeamOre()>750) || (rc.readBroadcast(3)<=2 && rc.getTeamOre()>750)){
            	//check how many miner factories
        		Direction dir = getBuildLocation(RobotType.MINERFACTORY);
            	if(dir != null){
            		buildUnit(RobotType.MINERFACTORY, dir);
            	}
            } else if(rc.getTeamOre()>200) {
            	Direction dir = getBuildDirection(RobotType.HANDWASHSTATION);
            	if(dir != null){
            		buildUnit(RobotType.HANDWASHSTATION, dir);
            	}
            } else{
            	moveRandomly();
            }*/
            rc.yield();
        }
    }

    public static class Barracks extends BaseBot {
        public Barracks(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class Soldier extends BaseBot {
        public Soldier(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class Tower extends BaseBot {
        public Tower(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
}