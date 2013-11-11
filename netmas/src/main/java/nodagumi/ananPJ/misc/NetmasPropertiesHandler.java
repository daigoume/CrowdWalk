package nodagumi.ananPJ.misc;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.Document;

import org.apache.commons.cli.*;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;
import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.misc.CommunicationHandler;
import nodagumi.ananPJ.misc.CommunicationHandler.CommunicationType;
import nodagumi.ananPJ.network.DaRuMaClient;


public class NetmasPropertiesHandler implements Serializable {

    private static final long serialVersionUID = 50125012L;

    public static final List cuiPropList = Arrays.asList(
            "debug",
            "io_handler_type",
            "map_file",
            "pollution_file",
            "scenario_file",
            "generation_file",
            "timer_enable",
            "timer_file",
            "interval",
            "addr",
            "port",
            "serialize_file",
            "serialize_interval",
            "deserialized_file",
            "randseed",
            "random_navigation",
            "speed_model",
            "expected_density_speed_model_macro_timestep",
            "time_series_log",
            "time_series_log_path",
            "time_series_log_interval",
            "loop_count",
            "exit_count",
            "all_agent_speed_zero_break"
            );

    protected String propertiescenarioPath = null;

    /**
     * Get a properties file name.
     * @return Property file name.
     */
    public String getPropertiescenarioPath() {
        return propertiescenarioPath;
    }

    /**
     * Set a properties file name.
     * @param _path a properties file name.
     */
    public void setPropertiescenarioPath(String _path) {
        propertiescenarioPath = _path;
    }

    protected boolean isDebug = false; /** debug mode */
    /**
     * Get a debug mode.
     * @return wether debug mode is enable or not
     */
    public boolean getIsDebug() {
        return isDebug;
    }

    protected CommunicationType communicationType = null; /** file or pipe or 
                                                          network */
    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    protected String mapPath = null; // path to map file (required)
    public String getMapPath() {
        return mapPath;
    }

    protected String pollutionPath = null; // path to pollution file
    public String getPollutionPath() {
        return pollutionPath;
    }

    protected String generationPath = null; // path to generation file
    public String getGenerationPath() {
        return generationPath;
    }

    protected String scenarioPath = null; // path to scenario file
    public String getScenarioPath() {
        return scenarioPath;
    }

    protected boolean isTimerEnabled = false;
    public boolean getIsTimerEnabled() {
        return isTimerEnabled;
    }

    protected String timerPath = null;         // path to timer log file
    public String getTimerPath() {
        return timerPath;
    }

    protected int interval = -1;       // sleep time(msec) during loop
    public int getInterval() {
        return interval;
    }

    protected String serializePath = null;    // path to serialized file
    public String getSerializePath() {
        return serializePath;
    }

    protected int serializeInterval = -1;  // interval of serialize
    public int getSerializeInterval() {
        return serializeInterval;
    }

    protected String addr = null;      // IP address of destination host
    public String getAddr() {
        return addr;
    }

    protected int port = -1;           // port number of destination host
    public int getPort() {
        return port;
    }

    protected String deserializePath = null;
    public String getDeserializePath() {
        return deserializePath;
    }

    protected long randseed = 0;
    public long getRandseed() {
        return randseed;
    }

    // enable random navigation on RunningAroundPerson
    protected boolean randomNavigation = false;
    public boolean getRandomNavigation() {
        return randomNavigation;
    }

    protected static SpeedCalculationModel speedModel = null;
    public SpeedCalculationModel getSpeedModel() {
        return speedModel;
    }

    protected static int expectedDensityMacroTimeStep = 300;
    public int getExpectedDensityMacroTimeStep() {
        return expectedDensityMacroTimeStep;
    }

    protected static boolean expectedDensityVisualizeMicroTimeStep = false;
    public boolean getExpectedDensityVisualizeMicroTimeStep() {
        return expectedDensityVisualizeMicroTimeStep;
    }

    // whether call NetworkMap.saveTimeSeriesLog in loop
    protected boolean isTimeSeriesLog = false;
    public boolean getIsTimeSeriesLog() {
        return isTimeSeriesLog;
    }
    protected String timeSeriesLogPath = null;
    public String getTimeSeriesLogPath() {
        return timeSeriesLogPath;
    }
    protected int timeSeriesLogInterval = -1;
    public int getTimeSeriesLogInterval() {
        return timeSeriesLogInterval;
    }

    // 
    protected boolean isDamageSpeedZero = false;
    public boolean getIsDamageSpeedZero() {
        return isDamageSpeedZero;
    }
    protected String damageSpeedZeroPath = null;
    public String getDamageSpeedZeroPath() {
        return damageSpeedZeroPath;
    }

    // End condition of simulation
    protected int exitCount = 0;
    public int getExitCount() {
        return exitCount;
    }

    protected int loopCount = -1;
    public int getLoopCount() {
        return loopCount;
    }

    protected boolean isAllAgentSpeedZeroBreak = false;
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    protected boolean isDeserialized = false;

    public NetmasPropertiesHandler(String _propertiescenarioPath) {
        // load properties
        Properties prop = new Properties();
        propertiescenarioPath = _propertiescenarioPath;
        try {
            System.err.println(_propertiescenarioPath);
            prop.loadFromXML(new FileInputStream(_propertiescenarioPath));
            isDebug = getBooleanProperty(prop, "debug");
            String typestr = getProperty(prop, "io_handler_type");
            if (typestr == null)
                communicationType = CommunicationType.SND_FILE;
            else if (typestr.equals("buffer"))
                communicationType = CommunicationType.SND_BUFFER;
            else if (typestr.equals("file"))
                communicationType = CommunicationType.SND_FILE;
            else if (typestr.equals("pipe"))
                communicationType = CommunicationType.SND_PIPE;
            else if (typestr.equals("network"))
                communicationType = CommunicationType.SND_NETWORK;
            else if (typestr.equals("server"))
                communicationType = CommunicationType.RCV_NETWORK;
            else if (typestr.equals("none"))
                communicationType = CommunicationType.NONE;
            else {
                System.err.println("NetmasPropertiesHandler: invalid " +
                        "inputted type:" + typestr);
                communicationType = CommunicationType.NONE;
            }
            // input files
            mapPath = getStringProperty(prop, "map_file");
            pollutionPath = getStringProperty(prop, "pollution_file");
            generationPath = getStringProperty(prop, "generation_file");
            scenarioPath = getProperty(prop, "scenario_file");
            // timer enabled or not
            isTimerEnabled = getBooleanProperty(prop, "timer_enable");
            if (isTimerEnabled)
                timerPath = getStringProperty(prop, "timer_file");

            // interval during main loop
            interval = getIntegerProperty(prop, "interval");
            // destination address if I/O handler is network mode
            addr = getStringProperty(prop, "addr");
            // port number
            port = getIntegerProperty(prop, "port");

            // scerialize file
            serializePath = getStringProperty(prop, "serialize_file");
            // interval of serialize (loop count)
            if (serializePath != null)
                serializeInterval = getIntegerProperty(prop,
                        "serialize_interval");
            // descerialize file
            deserializePath = getStringProperty(prop, "deserialized_file");
            // create random with seed
            randseed = getIntegerProperty(prop, "randseed");
            // random navigation
            randomNavigation = getBooleanProperty(prop, "random_navigation");
            // speed model
            String speedModelString = getStringProperty(prop, "speed_model");
            if (speedModelString.equals("density")) {
                speedModel = SpeedCalculationModel.DensityModel;
            } else if (speedModelString.equals("expected_density")) {
                speedModel = SpeedCalculationModel.ExpectedDensityModel;
                // macro time step of expected density speed model
                expectedDensityMacroTimeStep = getIntegerProperty(prop,
                        "expected_density_speed_model_macro_timestep");
            } else {
                speedModel = SpeedCalculationModel.LaneModel;
                System.err.println("NetmasCuiSimulator speed model: lane");
            }
            // time series log
            isTimeSeriesLog = getBooleanProperty(prop, "time_series_log");
            if (isTimeSeriesLog) {
                timeSeriesLogPath = getStringProperty(prop,
                        "time_series_log_path");
                timeSeriesLogInterval = getIntegerProperty(prop,
                        "time_series_log_interval");
            }
            // the number of agents with damaged speed zero
            isDamageSpeedZero = getBooleanProperty(prop,
                                                   "damage_speed_zero_log");
            if (isDamageSpeedZero) {
                damageSpeedZeroPath = getStringProperty(prop,
                        "damage_speed_zero_log_path");
            }
            // loop count
            loopCount = getIntegerProperty(prop, "loop_count");
            // exit count
            exitCount = getIntegerProperty(prop, "exit_count");
            isAllAgentSpeedZeroBreak = getBooleanProperty(prop,
                    "all_agent_speed_zero_break");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // check property options
        if (mapPath == null) {
            System.err.println("NetmasCuiSimulator: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(mapPath)).exists()) {
            System.err.println("NetmasCuiSimulator: specified map file does " +
                    "not exist.");
            return;
        } else if (communicationType == CommunicationType.SND_FILE &&
                serializePath == null) {
            System.err.println("NetmasCuiSimulator: file mode requires" +
                    " path to log.");
            return;
        } else if (communicationType == CommunicationType.SND_NETWORK &&
                addr == null) {
            System.err.println("NetmasCuiSimulator: network mode " +
                    "requires destination address.");
            return;
        }
    }

    public static String getProperty(Properties prop, String key) {
        if (prop.containsKey(key)) {
            return prop.getProperty(key);
        } else {
            return null;
        }
    }

    public static String getStringProperty(Properties prop, String key) {
        String stringProp = getProperty(prop, key);
        if (stringProp != null && !stringProp.equals(""))
            return stringProp;
        else {
            //System.err.println("string prop null: " + key);
            return null;
        }
    }

    public static boolean getBooleanProperty(Properties prop, String key) {
        String stringProp = getStringProperty(prop, key);
        if (stringProp == null) {
            //System.err.println("null: ");
            return false;
        } else if (stringProp.equals("true"))
            return true;
        else
            return false;
    }

    public static int getIntegerProperty(Properties prop, String key) {
        String stringProp = getStringProperty(prop, key);
        if (stringProp == null)
            return -1;
        else
            return Integer.parseInt(stringProp);
    }

    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("properties_file")
                .hasArg(true).withDescription("Path of properties file")
                .isRequired(true).create("p"));

        CommandLineParser parser = new BasicParser();
        CommandLine cli = null;

        try {
            cli = parser.parse(options, args);
        } catch (MissingOptionException moe) {
            moe.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NetmasPropertiesHandler", options, true);
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        String propertiescenarioPath = cli.getOptionValue("p");

        NetmasPropertiesHandler nph =
            new NetmasPropertiesHandler(propertiescenarioPath);
    }
}
