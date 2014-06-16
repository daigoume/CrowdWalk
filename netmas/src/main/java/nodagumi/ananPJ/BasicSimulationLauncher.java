package nodagumi.ananPJ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import nodagumi.ananPJ.misc.NetmasPropertiesHandler;
import nodagumi.ananPJ.misc.osmTools.osmNetworkMap;
import nodagumi.ananPJ.network.DaRuMaClient;


public abstract class BasicSimulationLauncher implements Serializable {
    private DaRuMaClient darumaClient = DaRuMaClient.getInstance();
    protected NetmasPropertiesHandler properties = null;

    protected NetworkMap readMapWithName(String file_name, Random _random)
            throws IOException {
        FileInputStream fis = new FileInputStream(file_name);
        Document doc = darumaClient.streamToDoc(fis);
        if (doc == null) {
            System.err.println("ERROR Could not read map.");
            return null;
        }
        NodeList toplevel = doc.getChildNodes();
        if (toplevel == null) {
            System.err.println("BasiciSimulationLauncher.readMapWithName " +
                    "invalid inputted DOM object.");
            return null;
        }
        _random.setSeed(properties.getRandseed());  // NetworkMap の生成時に random オブジェクトを初期化する(CUIモードとGUIモードでシミュレーション結果を一致させるため)
        // open street map
        if (toplevel.item(0).getNodeName().equals("osm")) {
            System.err.println("BasicSimulationLauncher read Open Street Map" +
                    " format map file.");
            osmNetworkMap network_map = new osmNetworkMap(_random);
            if (!((osmNetworkMap) network_map).fromDOM(doc))
                return null;
            return (NetworkMap) network_map;
        }
        // NetMAS based map
        NetworkMap network_map = new NetworkMap(_random);
        if (false == network_map.fromDOM(doc))
            return null;
        network_map.setFileName(file_name);
        return network_map;
    }
}
