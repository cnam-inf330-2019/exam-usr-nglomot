package net.cnam.inf330;

import java.util.*;

/**
 * Class for managing the rovers that are deployed on the Mars exploration grid.
 */
public final class MissionCommandCenter {
    private int gridWidth;
    private int gridHeight;
    private List<Rover> rovers;
    private int cheminRover;

    // TODO 1) Make MCC a singleton class
    private static volatile MissionCommandCenter instance =null;
    public static MissionCommandCenter getInstance() {
        if (instance == null) {
            synchronized (MissionCommandCenter.class) {
                if (instance == null) {
                    instance = new MissionCommandCenter();
                }
            }
        }
        return instance;
    }

    /**
     * Create a MCC without a predefined grid size.
     */
    private MissionCommandCenter() {
        this.gridWidth = -1;
        this.gridHeight = -1;
        this.rovers = new ArrayList<>();
    }

    /**
     * Create a MCC with a predefined grid size.
     *
     * @param gridWidth  The width (X axis) of the exploration grid
     * @param gridHeight The height (Y axis) of the exploration grid
     */
    private MissionCommandCenter(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.rovers = new ArrayList<>();
    }

    /**
     * Process the rover data line by line.
     *
     * @param lines The lines representing the rover data to process
     * @return The lines of output data describing the final positions of all managed Rovers.
     */
    public List<String> processRoverData(List<String> lines) {
        System.out.println("Processing rover data...");

        String[] splitFirstLine = lines.remove(0).split(" ");
        int gridWidth = Integer.parseInt(splitFirstLine[0]);
        int gridHeight = Integer.parseInt(splitFirstLine[1]);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        System.out.println("* Size of the grid : (" + this.gridWidth + "," + this.gridHeight + ")");

        List<String> outputLines = new ArrayList<>();
        int currentRoverId = 0;
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            currentRoverId++;
            cheminRover=0;
            String roverInitialStateData = it.next();
            String roverInstructionsData = it.next();

            Rover rover = deployAndMoveRover(currentRoverId, roverInitialStateData, roverInstructionsData);
            rovers.add(rover);
            System.out.println("Rover " + currentRoverId + "'s final state : " + rover);

            double roverCoveragePercent = computeRoverCoveragePercent(rover);
            System.out.println("Rover " + rover.getId() + "'s grid coverage : " + roverCoveragePercent + "%");

            outputLines.add(String.join(" ", Arrays.asList(Integer.toString(rover.getX()),
                    Integer.toString(rover.getY()), rover.getO().toString())));
        }

        System.out.println("Finished processing rover data.");
        return outputLines;
    }

    /**
     * Deploy a new rover on the grid at its initial position, and move it until it reaches its final position.
     *
     * @param roverId              The ID of the rover to move
     * @param roverInitialPosition The rover's initial position data
     * @param roverInstructions    The data containing set of instructions to move the rover
     * @return The newly deployed Rover at its final position
     */
    public Rover deployAndMoveRover(int roverId, String roverInitialPosition,
                                    String roverInstructions) {
        System.out.println("* Established communication signal with rover " + roverId + ".");

        String[] splitRoverInitialPositionData = roverInitialPosition.split(" ");
        int roverX = Integer.parseInt(splitRoverInitialPositionData[0]);
        int roverY = Integer.parseInt(splitRoverInitialPositionData[1]);
        Orientation roverOrientation = Orientation.valueOf(splitRoverInitialPositionData[2]);

        Rover rover = new Rover(roverId, roverX, roverY, roverOrientation);
        System.out.println("Rover " + roverId + "'s initial state : " + rover);

        try {
            checkRoverPosition(rover);
        } catch (InvalidRoverPositionException e) {
            // TODO 4) b) Don't deploy the rover if its initial position is invalid
            System.out.println("### WARNING : " + e.getMessage());
            System.out.println("Terminated communication with rover " + roverId + ".");
            return rover; // FIXME The rover should not be deployed, return null
        }

        System.out.println("Controlling rover " + roverId + "...");
        for (Character c : roverInstructions.toCharArray()) {
            rover.processCommand(RoverCommand.valueOf(String.valueOf(c)));
            cheminRover++; // FIXME This isn't the rover coverage, but just the number of moves
            // TODO FIXME 4) a) Make the rover pull back if the move is invalid
            try {
                checkRoverPosition(rover);
            } catch (InvalidRoverPositionException e) {
                System.out.println("### WARNING : " + e.getMessage());
            }
        }
        System.out.println("Terminated communication with rover " + roverId + ".");
        return rover;
    }

    /**
     * Check whether the rover's current position is valid.
     *
     * @param rover The rover whose position must be checked
     * @throws InvalidRoverPositionException
     */
    public void checkRoverPosition(Rover rover) throws InvalidRoverPositionException {
        if (rover.getX() > this.gridWidth || rover.getY() > this.gridHeight)
            throw new InvalidRoverPositionException(rover,
                    "Position out of grid ! Communication signal weak.");

        // TODO 2) Throw an InvalidRoverPositionException if there is another rover on the rover's current position.
        for(int i = 0; i<rovers.size(); i++){
            if(rover.getX()==rovers.get(i).getX() & rover.getY()==rovers.get(i).getY()){
                // FIXME The move backwards should be in the caller method, not here.
                // FIXME This method only checks and throws errors when needed
                rover.moveBackward();
                throw new InvalidRoverPositionException(rover,
                        "Position not clear ! Initailizing pull back !");
            }
        }
    }

    /**
     * Compute the rover's coverage percent, which is defined by the ratio between the number of distinct positions
     * visited by the rover and the total number of positions on the exploration grid.
     *
     * @param rover The Rover whose coverage percent is computed
     * @return The rover's coverage percent as a double
     */
    public double computeRoverCoveragePercent(Rover rover) {
        // TODO 6) Compute the rover's grid coverage percentage
        double gridArea = getGridHeight()*getGridWidth();
        /* FIXME Wrong calculation because cheminRover isn't the number of distinct positions visited by the rover,
            but simply the total number of moves made by the rover.
         */

        return cheminRover/gridArea*100;
    }

    /**
     * Add a new rover to be managed by the MCC.
     *
     * @param rover The new Rover to manage
     */
    public void addRover(Rover rover) {
        this.rovers.add(rover);
    }

    /**
     * Remove all rovers from the the set of rovers managed by the MCC.
     */
    public void clearRovers() {
        this.rovers.clear();
    }

    /**
     * Get the width (X axis) of the exploration grid.
     *
     * @return The width of the grid as an integer
     */
    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * Get the height (Y axis) of the exploration grid.
     *
     * @return The height of the grid as an integer
     */
    public int getGridHeight() {
        return gridHeight;
    }

    /**
     * Get the list of rovers currently managed by the MCC.
     *
     * @return A list of Rovers
     */
    public List<Rover> getRovers() {
        return rovers;
    }
}
