package washyourhands;

import battlecode.common.*;

import java.util.*;


public class RobotPlayer {
	static int maxBeavers=2;
	static int maxMinerfactories=3;
	static int maxMiners=50;
	static int maxBarracks=2;
	static int maxHelipads=1;
	static int maxTankfactories=4;
	static int maxAerospacelabs=20;
	static int maxSupplydepots=30;
	static Direction facing;
	static Random rand;
	static RobotController rc;
	
	public static boolean needsSupplier(RobotController rc) throws GameActionException {
		if (rc.readBroadcast(200) == 0) {
			return true;
		}
		return false;
	}
	
	public static void run(RobotController myrc) throws GameActionException {
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
            myself = new Tank(rc);
        } else if (rc.getType() == RobotType.DRONE) {
            myself = new Drone(rc);
            rc.broadcast(199, rc.getID());
        } else if (rc.getType() == RobotType.MINERFACTORY) {
        	myself = new MinerFactory(rc);
        } else if (rc.getType() == RobotType.TANKFACTORY) {
            myself = new TankFactory(rc);
        } else if (rc.getType() == RobotType.AEROSPACELAB) {
            myself = new AerospaceLab(rc);
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
        double buildingOre;
        double movingThingyOre;
        public static MapLocation[] friendlyTowerLocs;

        public BaseBot(RobotController rc) {
            this.rc = rc;
            this.myHQ = rc.senseHQLocation();
            this.theirHQ = rc.senseEnemyHQLocation();
            this.myTeam = rc.getTeam();
            this.theirTeam = this.myTeam.opponent();
            BaseBot.towers=rc.senseTowerLocations().length;
            BaseBot.friendlyTowerLocs=rc.senseTowerLocations();
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

    	public boolean spawnUnit(RobotType type, Direction dir) throws GameActionException {
    		if(rc.isCoreReady()&&rc.canSpawn(dir, type)){
    			rc.spawn(dir, type);
    			return true;
    		} 
    		return false;
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
            Direction[] dirs = {Direction.NORTH, Direction.NORTH_WEST, Direction.EAST, Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST};
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
       

		public RobotInfo[] getAllies() {
            RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
            return allies;
        }

        public RobotInfo[] getEnemiesInAttackingRange() {
            RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, theirTeam);
            return enemies;
        }
        
        public MapLocation getClosestToEnemyHQ(MapLocation[] locs){
        	int min = 9999999;
        	int dist;
        	MapLocation minLoc=null;
        	for (int i=0; i<locs.length; i++){
        		MapLocation myLoc=locs[i];
        		dist = locs[i].distanceSquaredTo(this.theirHQ);
        		if(dist<min){
        			min=dist;
					minLoc=myLoc;
        		}
        	}
        	return minLoc;
        }
        
        public MapLocation getClosestToHQ(MapLocation[] locs){
        	int min = 9999999;
        	int dist;
        	MapLocation minLoc=null;
        	for (int i=0; i<locs.length; i++){
        		MapLocation myLoc=locs[i];
        		dist = locs[i].distanceSquaredTo(this.myHQ);
        		if(dist<min){
        			min=dist;
					minLoc=myLoc;
        		}
        	}
        	return minLoc;
        }
        
        public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
            if (enemies.length == 0) {
                return;
            }

            double minEnergon = Double.MAX_VALUE;
            MapLocation toAttack = null;
            for (RobotInfo info : enemies) {
            	if(info.type==RobotType.TOWER){
            		toAttack=info.location;
            		break;
            	}
            		
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
                if(Clock.getRoundNum()<500){
	                this.buildingOre= ((double) rc.getTeamOre())*.5;
	                this.movingThingyOre= ((double) rc.getTeamOre())*.5;
                }
                this.buildingOre= ((double) rc.getTeamOre())*.95;
                this.movingThingyOre= ((double) rc.getTeamOre())*.05;
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
        	friendlyTowerLocs=rc.senseTowerLocations();
        	
        	RobotInfo[] enemies = getEnemiesInAttackingRange();
        	
        	if(enemies.length>0){
		        	if(rc.isWeaponReady()){
		        		if(friendlyTowerLocs.length>=5)
		        			attackClump();
		        		else
		        			attackLeastHealthEnemy(enemies);
		        		rc.broadcast(91,1);
	        	} else{
	        		rc.broadcast(91, 0);
	        	}
        	}
        	
        	if(rc.readBroadcast(2)<maxBeavers){
        		if(rc.getTeamOre()>200 && spawnUnit(RobotType.BEAVER, getSpawnDirection(RobotType.BEAVER))){
        			rc.broadcast(2, rc.readBroadcast(2) + 1);
        		}
        	}

            MapLocation rallyPoint=null;
            
            if (numFighters()>50 || Clock.getRoundNum()>1700 && numFighters()>30){   // can attack HQ
            	rallyPoint = this.theirHQ;
            }
            else if(numFighters()>20){ //can attack Tower
            	MapLocation[] enemyTowers =rc.senseEnemyTowerLocations();
            	if(enemyTowers.length>0){
            		rallyPoint = getClosestToHQ(enemyTowers);
            	}
            } 
            
            if(rallyPoint!=null){
	            rc.broadcast(0, rallyPoint.x);
	            rc.broadcast(1, rallyPoint.y);
            } else{
	            rc.broadcast(0, 0);
	            rc.broadcast(1, 0);
            }
            
            if(!isFinished){
                analyzeMap();
                analyzeTowers();
            }
            else{
                chooseStrategy();
            }
            
            rc.yield();
        }
        
        private void attackClump() {
			 
			
		}

		public int numFighters(){
        	RobotInfo[] myRobots = rc.senseNearbyRobots(999999, myTeam);
        	int numFighters = 0;
			for (RobotInfo r : myRobots) {
				RobotType type = r.type;
				if (type == RobotType.SOLDIER) {
					numFighters++;
				} else if (type == RobotType.TANK) {
					numFighters++;
				} else if (type == RobotType.DRONE) {
					numFighters++;
				} else if (type == RobotType.LAUNCHER) {
					numFighters++;
				}
			}
			return numFighters;
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
            if(totalProcessed != 0)
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
        	if(buildingOre> 500 && rc.readBroadcast(4)<1){
	        		if(buildLoc==null)
	        			buildLoc = getBuildLocation(RobotType.MINERFACTORY);
	        		buildLoc = buildUnit(RobotType.MINERFACTORY, buildLoc);
	        		if(buildLoc==null){
	        			rc.broadcast(4, 1);
	        			buildingOre-=500;
	        		}
        	}
        	
        	if(buildingOre>600 && rc.readBroadcast(11)<maxAerospacelabs && rand.nextDouble()>.5 && Clock.getRoundNum()>500 ){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.AEROSPACELAB);
        		buildLoc = buildUnit(RobotType.AEROSPACELAB, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(5, rc.readBroadcast(11)+1);
        			buildingOre-=600;
        		}
        	}
        	
        	if(buildingOre>300 && Clock.getRoundNum()>1000 && rand.nextDouble()>.85 && rc.readBroadcast(10)<maxSupplydepots){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.SUPPLYDEPOT);
        		buildLoc = buildUnit(RobotType.SUPPLYDEPOT, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(10, rc.readBroadcast(10)+1);
        			buildingOre-=300;
        		}
        	}
        	
        	if(buildingOre>300 && (rc.readBroadcast(5)<maxBarracks && rand.nextDouble()>.15 && Clock.getRoundNum()>1000 || rc.readBroadcast(5)<1 && rc.readBroadcast(4)>0) ){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.BARRACKS);
        		buildLoc = buildUnit(RobotType.BARRACKS, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(5, rc.readBroadcast(5)+1);
        			buildingOre-=300;
        		}
        	}
        	if(buildingOre>500 && (rc.readBroadcast(7)<maxTankfactories && rand.nextDouble()>.50 && Clock.getRoundNum()>1000 || rc.readBroadcast(7)<1 && rc.readBroadcast(5)>0) ){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.TANKFACTORY);
        		buildLoc = buildUnit(RobotType.TANKFACTORY, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(7, rc.readBroadcast(7)+1);
        			buildingOre-=500;
        		}
        	}
        	
        	if(buildingOre>300 && (rc.readBroadcast(8)<maxHelipads && rand.nextDouble()>.2 && Clock.getRoundNum()>1000 || rc.readBroadcast(8)<1 && rc.readBroadcast(4)>0) ){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.HELIPAD);
        		buildLoc = buildUnit(RobotType.HELIPAD, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(8, rc.readBroadcast(8)+1);
        			buildingOre-=300;
        		}
        	}
        	
        	if(buildingOre>500 && (rc.readBroadcast(11)<maxAerospacelabs && rand.nextDouble()>.85 || rc.readBroadcast(11)<1) ){
        		if(buildLoc==null)
        			buildLoc = getBuildLocation(RobotType.AEROSPACELAB);
        		buildLoc = buildUnit(RobotType.AEROSPACELAB, buildLoc);
        		if(buildLoc==null){
        			rc.broadcast(11, rc.readBroadcast(11)+1);
        			buildingOre-=500;
        		}
        	}
        	
            rc.yield();
        }
        
    	public MapLocation buildUnit(RobotType type, MapLocation buildLoc) throws GameActionException {
        	Direction moveDir = getMoveDir(buildLoc);
        	MapLocation myLoc = rc.getLocation();
        	Direction buildDir = myLoc.directionTo(buildLoc);
        	boolean adj = myLoc.isAdjacentTo(buildLoc);
        	
        	if(adj){
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

    }

    public static class Miner extends BaseBot {
        public Miner(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            mineAndMove();
       
            rc.yield();
        }
        
        public void mineAndMove() throws GameActionException {
            MapLocation toMove = rc.getLocation();
            double ore = rc.senseOre(toMove);
            MapLocation[] possibleBlocks = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 1);
            for(MapLocation ml : possibleBlocks){
                if(rc.senseTerrainTile(ml) == TerrainTile.NORMAL && !rc.isLocationOccupied(ml) && rc.senseOre(ml) > ore){
                    toMove = ml;
                    ore = rc.senseOre(ml);
                }
            }

            int robs = 0;
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(toMove, 1, rc.getTeam());
            for(RobotInfo ri : nearbyAllies){
                if(ri.type == RobotType.MINER)
                    robs++;
            }

            if(ore >= rc.readBroadcast(6) && robs < rc.readBroadcast(9)){
                rc.broadcast(6, (int)ore);
                rc.broadcast(9, robs);
                rc.broadcast(10, toMove.x);
                rc.broadcast(11, toMove.y);
            }
            if(rc.isCoreReady()){
    	        if(toMove == rc.getLocation() && ore > 40){
    	            rc.mine();
    	        }
    	        else if(ore > 40)
    	            rc.move(rc.getLocation().directionTo(toMove));
    	        else{
    	            MapLocation bestBlock = new MapLocation(rc.readBroadcast(10), rc.readBroadcast(11));
    	            rc.move(rc.getLocation().directionTo(bestBlock));
    	        }
            }
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
    	private MapLocation defending;
    	
        public Soldier(RobotController rc) {
            super(rc);
            MapLocation[] friendlyTowerLocs = rc.senseTowerLocations();
            int index = 0;
            index = (int)(rand.nextDouble()*((double)friendlyTowerLocs.length));
            if(friendlyTowerLocs.length>0)
            	defending = friendlyTowerLocs[index];
            else
            	defending = this.myHQ;
        }

        public void execute() throws GameActionException {
            RobotInfo[] enemies = getEnemiesInAttackingRange();

            if (enemies.length > 0) {
                //attack!
                if (rc.isWeaponReady()) {
                    attackLeastHealthEnemy(enemies);
                }
            }
            else if (rc.isCoreReady()) {
                int rallyX = rc.readBroadcast(0);
                int rallyY = rc.readBroadcast(1);
                
                MapLocation moveTo;
                
                if(rallyX==0 && rallyY==0)
                	moveTo= new MapLocation(rallyX, rallyY);
                else
                	moveTo = defending;
                	
                Direction newDir = getMoveDir(moveTo);

                if (newDir != null) {
                	//add move safely method
                    rc.move(newDir);
                }
            }
            rc.yield();
        }
    }

    public static class Tower extends BaseBot {
        public Tower(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	if(rc.isWeaponReady() && !rc.isCoreReady())
        		attackLeastHealthEnemy(getEnemiesInAttackingRange());
        }
        
    }
    
    public static class Tank extends BaseBot {
    	private MapLocation defending;
    	
        public Tank(RobotController rc) {
            super(rc);
            MapLocation[] friendlyTowerLocs = rc.senseTowerLocations();
            int index = 0;
            index = (int)(rand.nextDouble()*((double)friendlyTowerLocs.length));
            if(friendlyTowerLocs.length>0)
            	defending = friendlyTowerLocs[index];
            else
            	defending = this.myHQ;
        }

        public void execute() throws GameActionException {
            RobotInfo[] enemies = getEnemiesInAttackingRange();


            if (enemies.length > 0) {
                //attack!
                if (rc.isWeaponReady()) {
                    attackLeastHealthEnemy(enemies);
                }
            }
            else if (rc.isCoreReady()) {
                int rallyX = rc.readBroadcast(0);
                int rallyY = rc.readBroadcast(1);
                
                MapLocation moveTo;
                
                if (rallyX!=0 && rallyY!=0)
                	moveTo= new MapLocation(rallyX, rallyY);
                else{
                	moveTo=defending;
                }
                	
                Direction newDir = getMoveDir(moveTo);

                if (newDir != null) {
                	//add move safely method
                    rc.move(newDir);
                }
            }
            rc.yield();
        }
    }
    
    public static class Launcher extends BaseBot {
    	private MapLocation defending;
        public Launcher(RobotController rc) {
            super(rc);
            MapLocation[] friendlyTowerLocs = rc.senseTowerLocations();
            System.out.println(friendlyTowerLocs);
            int index = 0;
            index = (int)(rand.nextDouble()*((double)friendlyTowerLocs.length));
            if(friendlyTowerLocs.length>0)
            	defending = friendlyTowerLocs[index];
            else
            	defending = this.myHQ;
        }

        public void execute() throws GameActionException {
            RobotInfo[] enemies = getEnemiesInAttackingRange();

            if (enemies.length > 0) {
                //attack!
                if (rc.isWeaponReady()) {
                    attackLeastHealthEnemy(enemies);
                }
            }
            else if (rc.isCoreReady()) {
                int rallyX = rc.readBroadcast(0);
                int rallyY = rc.readBroadcast(1);
                
                MapLocation moveTo;
                
                if(rallyX==0 && rallyY==0)
                	moveTo= new MapLocation(rallyX, rallyY);
                else
                	moveTo = defending;
                	
                Direction newDir = getMoveDir(moveTo);

                if (newDir != null) {
                	//add move safely method
                    rc.move(newDir);
                }
            }
            rc.yield();
        }
    }

    public static class Drone extends BaseBot {
    	private MapLocation defending;
    	
        public Drone(RobotController rc) {
            super(rc);
            int index = (int)(rand.nextDouble()*((double)friendlyTowerLocs.length));
            defending = friendlyTowerLocs[index];
        }

        public void execute() throws GameActionException {
            int queueStart = rc.readBroadcast(298), queueEnd = rc.readBroadcast(299);

            if (rc.isCoreReady()) {
                if (queueStart != queueEnd && rc.getSupplyLevel() > 1000) {
                    RobotInfo[] allies = getAllies();

                    int target = rc.readBroadcast(queueStart);

                    for (int i=0; i<allies.length; ++i) {
                        if (allies[i].ID == target) {
                            if (rc.getLocation().distanceSquaredTo(allies[i].location) <= GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
                                rc.transferSupplies(10000, allies[i].location);
                                rc.broadcast(298, queueStart+1);
                            }
                            else {
                                Direction toGoDir = getMoveDir(allies[i].location);

                                if (toGoDir != null) {
                                    rc.move(toGoDir);
                                }
                            }
                            break;
                        }
                    }
                }
                if (rc.getSupplyLevel() <= 1000) {
                    Direction toGoDir = getMoveDir(this.myHQ);

                    if (toGoDir != null) {
                        rc.move(toGoDir);
                    }
                }
            }
            
            rc.yield();
        }
    }

    public static class MinerFactory extends BaseBot {
        public MinerFactory(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            if(movingThingyOre > 60){
            	if(movingThingyOre > 1500){
            		Direction dir = getSpawnDirection(RobotType.MINER);
	        		if (dir!=null){
	        			spawnUnit(RobotType.MINER, getSpawnDirection(RobotType.MINER));
	        			movingThingyOre-=60;
	        		}
            	}
            	else{
            		if(Math.random() > 0.5){
		            	Direction dir = getSpawnDirection(RobotType.MINER);
		        		if (dir!=null){
		        			spawnUnit(RobotType.MINER, getSpawnDirection(RobotType.MINER));
		        			movingThingyOre-=60;
		        		}
            		}
            	}
            }
            rc.yield();
        }
    }

    public static class TankFactory extends BaseBot {
        public TankFactory(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
        	if(movingThingyOre > 250){
            	if(movingThingyOre > 1500){
            		Direction dir = getSpawnDirection(RobotType.TANK);
	        		if (dir!=null){
	        			spawnUnit(RobotType.TANK, getSpawnDirection(RobotType.TANK));
	        			movingThingyOre-=250;
	        		}
            	}
            	else{
            		if(Math.random() > 0.3){
		            	Direction dir = getSpawnDirection(RobotType.TANK);
		        		if (dir!=null){
		        			spawnUnit(RobotType.TANK, getSpawnDirection(RobotType.TANK));
		        			movingThingyOre-=250;
		        		}
            		}
            	}
            }
            rc.yield();
        }
    }

    public static class Helipad extends BaseBot { //2
        public Helipad(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            if(rc.readBroadcast(71) < 2 && movingThingyOre > 125){
            	Direction dir = getSpawnDirection(RobotType.DRONE);
            	if (dir!=null){
            		spawnUnit(RobotType.DRONE, dir);
            		rc.broadcast(71, rc.readBroadcast(71)+1);
            		movingThingyOre-=125;
            	}
            }
            rc.yield();
        }
    }

    public static class AerospaceLab extends BaseBot {
        public AerospaceLab(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            if(movingThingyOre > 400){
            	if(movingThingyOre > 1500){
            		Direction dir = getSpawnDirection(RobotType.LAUNCHER);
	        		if (dir!=null){
	        			spawnUnit(RobotType.LAUNCHER, getSpawnDirection(RobotType.LAUNCHER));
	        			movingThingyOre-=400;
	        		}
            	}
            	else{
            		if(Math.random() < 0.9){
		            	Direction dir = getSpawnDirection(RobotType.LAUNCHER);
		        		if (dir!=null){
		        			spawnUnit(RobotType.LAUNCHER, getSpawnDirection(RobotType.LAUNCHER));
		        			movingThingyOre-=400;
		        		}
            		}
            	}
            }
            rc.yield();
        }
    }
}