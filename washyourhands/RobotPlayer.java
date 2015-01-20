package washyourhands;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	static int maxBeavers=1;
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
        } else if (rc.getType() == RobotType.MINER) {
            myself = new Miner(rc);
        } else if (rc.getType() == RobotType.TANK) {
            myself = new Miner(rc);
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
        static int towers;

        public BaseBot(RobotController rc) {
            this.rc = rc;
            this.myHQ = rc.senseHQLocation();
            this.theirHQ = rc.senseEnemyHQLocation();
            this.myTeam = rc.getTeam();
            this.theirTeam = this.myTeam.opponent();
            BaseBot.towers=rc.senseTowerLocations().length;
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
        
    	public boolean spawnUnit(RobotType type, Direction dir) throws GameActionException {
    		if(rc.isCoreReady()&&rc.canSpawn(dir, type)){
    			rc.spawn(dir, type);
    			return true;
    		} 
    		return false;
    	}
    	public MapLocation buildUnit(RobotType type, MapLocation buildLoc) throws GameActionException {
        	Direction moveDir = getMoveDir(buildLoc);
        	MapLocation myLoc = rc.getLocation();
        	Direction buildDir = myLoc.directionTo(buildLoc);
        	boolean adj = myLoc.isAdjacentTo(buildLoc);
        	
        	if(adj){
        		System.out.println("imhere!");
    			if(rc.getTeamOre()>type.oreCost && rc.isCoreReady() &&rc.canBuild(buildDir, type)){
    				rc.build(buildDir,type);
    				return null;
    			} else{
        			return buildLoc;
        		}
        		
        	}
        	else if(rc.isCoreReady() && rc.canMove(moveDir)){
    			rc.move(moveDir);
    			return buildLoc;
    		}
        	
        	return buildLoc;
    		
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
            while(r<30){ //keep expanding out from the HQ till you find a valid location
            	MapLocation[] nearLocs=MapLocation.getAllMapLocationsWithinRadiusSq(loc, r);
            	for(MapLocation l : nearLocs){
            		if(!rc.isLocationOccupied(l) && isValidBuildLoc(l)){
            			return l;
            		}
            	}
            	r+=1;
            }
            return null;
        }

        private boolean isValidBuildLoc(MapLocation loc) throws GameActionException {
        	Direction[] directions = {Direction.NORTH,  Direction.EAST, Direction.SOUTH, Direction.WEST};
			for(Direction dir: directions){
				MapLocation newLoc=loc.add(dir);
				if(newLoc!=rc.getLocation()){
					if(rc.canSenseLocation(newLoc)){
						if(rc.isLocationOccupied(newLoc) || rc.senseTerrainTile(newLoc) != TerrainTile.NORMAL){
							return false;
						}
					}
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
        
        public void attackMostHealthEnemy(RobotInfo[] enemies) throws GameActionException {
            if (enemies.length == 0) {
                return;
            }

            double maxEnergon = Double.MIN_VALUE;
            MapLocation toAttack = null;
            for (RobotInfo info : enemies) {
                if (info.health > maxEnergon) {
                    toAttack = info.location;
                    maxEnergon = info.health;
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
        public static int xMin, xMax, yMin, yMax;
        public static int xpos, ypos;
        public static int totalNormal, totalVoid, totalProcessed;
        public static int towerThreat;

        public static double ratio;
        public static boolean isFinished;

        public static int strategy; // 0 = "defend", 1 = "build drones", 2 = "build soldiers"   	
    	
    	
        public HQ(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	if(rc.isWeaponReady()){
        		if(towers < 5){
        			RobotInfo[] enemies = getEnemiesInAttackingRange();
        			attackMostHealthEnemy(enemies);
        		}
        	}
        	
        	if(rc.readBroadcast(2)<maxBeavers){
        		if(rc.getTeamOre()>200 && spawnUnit(RobotType.BEAVER, getSpawnDirection(RobotType.BEAVER))){
        			rc.broadcast(2, rc.readBroadcast(2) + 1);
        		}
        	}
            
            MapLocation rallyPoint;
            if (Clock.getRoundNum() < 1000) {
                rallyPoint = new MapLocation( (this.myHQ.x + this.theirHQ.x) / 2,
                                              (this.myHQ.y + this.theirHQ.y) / 2);
            }
            else {
                rallyPoint = this.theirHQ;
            }
            rc.broadcast(0, rallyPoint.x);
            rc.broadcast(1, rallyPoint.y);
            
            if(!isFinished){
                analyzeMap();
                analyzeTowers();
            }
            else{
                chooseStrategy();
            }
            
            rc.yield();
        }

        public void analyzeMap(){
            while(ypos < yMax + 1){
                TerrainTile t = rc.senseTerrainTile(new MapLocation(xpos,ypos));
                if(t==TerrainTile.NORMAL){
                    totalNormal++;
                    totalProcessed++;
                }
                else if(t==TerrainTile.VOID){
                    totalVoid++;
                    totalProcessed++;
                }
                xpos++;
                if(xpos == xMax+1){
                    xpos = xMin;
                    ypos++;
                }
                
                if(Clock.getBytecodesLeft()<100)
                    return;
            }
            ratio = (double)(totalNormal/totalProcessed);
            isFinished = true;
        }

        public void analyzeTowers(){
            MapLocation[] towers = rc.senseEnemyTowerLocations();
            
            for(int i = 0; i < towers.length; ++i){
                MapLocation tower = towers[i];
                
                if((xMin <= tower.x && tower.x <= xMax) && (yMin <= tower.y && tower.y <=yMax) || tower.distanceSquaredTo(this.theirHQ) <= 50){
                    for(int j = 0; j < towers.length; ++j){
                        if(towers[j].distanceSquaredTo(tower) <= 50)
                            towerThreat++;
                    }
                }
            }
        }

        public void chooseStrategy() throws GameActionException{
            if(towerThreat >= 10){
                //defensive
                strategy = 0;
            }
            else{
                if(ratio <= 0.05){
                    //build drones
                    strategy = 1;
                }
                else{
                    //build soldiers
                    strategy = 2;
                }
            }
            rc.broadcast(100, strategy);
        }
    }

    public static class Beaver extends BaseBot {
    	MapLocation buildLoc=null;
        public Beaver(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	//builds in checkerboard pattern
        	if(rc.getTeamOre()>400){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.HANDWASHSTATION);
        		buildLoc = buildUnit(RobotType.HANDWASHSTATION, buildLoc);
        	}
        	
            rc.yield();
        }
    }

    public static class Miner extends BaseBot {
        public Miner(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            attackLeastHealthEnemy(getEnemiesInAttackingRange());
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