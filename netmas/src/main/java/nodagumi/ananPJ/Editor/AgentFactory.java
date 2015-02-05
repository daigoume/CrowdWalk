package nodagumi.ananPJ.Editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.GetDoublesDialog;
import nodagumi.ananPJ.misc.MapChecker;
import nodagumi.ananPJ.misc.GenerateAgent;

public class AgentFactory extends JPanel implements ItemListener,
       Serializable {
    private static final long serialVersionUID = -2940988288159998663L;
    JComboBox agentTypeList = null;
    private JPanel paramPanel = null;

    AgentBase agentCandidate = null;
    NetworkMapEditor editor = null;
    Random random;

    public AgentFactory (NetworkMapEditor _editor, Random _random) {
        super();
        random = _random;
        editor = _editor;
        setpp_panel();
        refresh();
    }
    
    private void setpp_panel() {
        setLayout(new BorderLayout());

        /* List of Agent types */
		/* [2015.01.04 I.Noda]
		 * 以下の、Agent生成機能は、今後使われない。
		 * なので、機能しなくて良い
		 * 山下さんに確認済み。
		 */
        agentTypeList =
			new JComboBox(GenerateAgent.getKnownAgentClassNameList()) ;
        agentTypeList.setEditable(false);
        agentTypeList.addItemListener(this);
        /*
        agentTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        agentTypeList.setPreferredSize(new Dimension(1, 1));
        JScrollPane scrollPane = new JScrollPane(agentTypeList);
        contentPane.add(scrollPane, BorderLayout.NORTH);
        */
        add(agentTypeList, BorderLayout.NORTH);

        /* AgentSettings */
        paramPanel = new JPanel();

        /* ok and cancel button */
        paramPanel.add(new JLabel("Not selected"));
        add(paramPanel, BorderLayout.CENTER);
        
        setPreferredSize(new Dimension(300, 220));
    }

    public AgentBase moveAgent(MapLink link, double position) {
        /* make agent according to the current agent template */
        if (agentCandidate != null) {
			agentCandidate.place(link, null, position);
        }
        return agentCandidate;
    }
    
    public boolean confirmAgentAttributes() {
        int i = JOptionPane.showConfirmDialog(null,
                "Really generate agents?",
                "Generate this agent", JOptionPane.YES_NO_OPTION);
        return (i == JOptionPane.YES_OPTION);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        refresh();
        repaint();
    }
    
    public void refresh() {
        String type = (String)agentTypeList.getSelectedItem();

        remove(paramPanel);
        agentCandidate = AgentBase.createEmptyAgent(type, random);
        if (agentCandidate == null) {
            paramPanel = new JPanel();
            paramPanel.add(new JLabel("Not selected"));
            paramPanel.repaint();
            add(paramPanel, BorderLayout.SOUTH);
        } else {
            paramPanel = agentCandidate.paramSettingPanel(editor.getMap());
            paramPanel.repaint();
            add(paramPanel, BorderLayout.SOUTH);
        }   
    }

	public boolean placeAgentsRandomly(String tag, String targetTag) {
        MapLinkTable links = editor.getMap().getLinks(); 
        MapNodeTable nodes = editor.getMap().getNodes(); 

        if (tag.equals("")) {
            System.err.println("empty tag, assuming null");
            tag = null;
        }
        if (links.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "You do not have any links to place agents on!",
                    "Not ready for placing agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (moveAgent(links.get(0), 0.0) == null) {
            JOptionPane.showMessageDialog(null,
                    "Please set the type of agents you want to place first.",
                    "Not ready for placing agents",
                    JOptionPane.WARNING_MESSAGE);
            //networkMapEditor.switchTab(NetworkMapEditor.TabTypes.AGENT);
            return false;
        }
        
        final String[] labels = {"NumAgent", "MinHeight", "MaxHeight"};
        final double[][] range = {{-20, 100000, 100},
                {-1000, 1000, 0},
                {-1000, 1000, 1000}};
        double[] ret = GetDoublesDialog.showDialog("", labels, range);
        if (ret == null) return false;
        //agentFactory.refresh();
        if (!confirmAgentAttributes()) return false;
        
        int numAgents = (int)ret[0];
        double minHeight = ret[1];
        double maxHeight = ret[2];

		MapLinkTable reachableLinks = MapChecker.getReachableLinks(nodes,
																   targetTag);
        if (reachableLinks.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No links reachable from the exit.\n"
                    + "Make sure that the exits are set.",
                    "Cannot place agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    
        MapLinkTable linksToPlace = new MapLinkTable();
    
        for (MapLink link : reachableLinks) {
            if (link.isBetweenHeight(minHeight, maxHeight)) {
                if (link.hasTag("LIFT")) continue;
                if (link.hasTag("STAIR")) continue;
                if (tag != null && !link.hasTag(tag)) continue;
                if (link.getFrom().getHeight() != link.getTo().getHeight()) continue;
                linksToPlace.add(link);
            }
        }

        if (linksToPlace.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No link to place agents.",
                    "Cannot place agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    
        for (int i = 0; i < numAgents; ++i) {
            //int lId = (int)(Math.random() * linksToPlace.size());
			int lId = random.nextInt(linksToPlace.size()) ;
            MapLink link = linksToPlace.get(lId);
            //double position = Math.random() * link.length;
            double position = random.nextDouble() * link.length;
            AgentBase agent = moveAgent(link, position);
            editor.getMap().addAgent((MapPartGroup)link.getParent(), agent.copyAndInitialize());
        }
        JOptionPane.showMessageDialog(null,
                "Placed " + numAgents + "agents.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

	public boolean placeAgentsEvenly(String tag, String targetTag) {
        MapLinkTable links = editor.getMap().getLinks(); 
        MapNodeTable nodes = editor.getMap().getNodes(); 
        if (links.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "You do not have any links to place agents on!",
                    "Not ready for placing agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (moveAgent(links.get(0), 0.0) == null) {
            JOptionPane.showMessageDialog(null,
                    "Please set the type of agents you want to place first.",
                    "Not ready for placing agents",
                    JOptionPane.WARNING_MESSAGE);
            //switchTab(TabTypes.AGENT);
            return false;
        }
    
        final String[] labels = {"NumAgentPerLink", "MinHeight", "MaxHeight"};
        final double[][] range = {{0, 100000, 10},
                {-1000, 1000, -1000},
                {-1000, 1000, 1000}};
        double[] ret = GetDoublesDialog.showDialog("", labels, range);
        if (ret == null) return false;              
        if (!confirmAgentAttributes()) return false;

        int numAgents = (int)ret[0];
        double minHeight = ret[1];
        double maxHeight = ret[2];
    
		MapLinkTable reachableLinks = MapChecker.getReachableLinks(nodes,
																   targetTag);
        if (reachableLinks.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No links reachable from the exit.\n"
                    + "Make sure that the exits are set.",
                    "Cannot place agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    
        MapLinkTable linksToPlace = new MapLinkTable();
    
        for (MapLink link : reachableLinks) {
            if (link.isBetweenHeight(minHeight, maxHeight)) {
                if (link.hasTag("LIFT")) continue;
                if (link.hasTag("STAIR")) continue;
                if (tag != null && !link.hasTag(tag)) continue;
                if (link.getFrom().getHeight() != link.getTo().getHeight()) continue;
                linksToPlace.add(link);
            }
        }
    
        if (linksToPlace.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No link to place agents.",
                    "Cannot place agents",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
                
        for (MapLink link : linksToPlace) {
            for (int i = 0; i < numAgents; ++i) {
                double position = (i + 1) * link.length / (numAgents + 1);
                AgentBase agent = moveAgent(link, position);
                editor.getMap().addAgent((MapPartGroup)link.getParent(), agent.copyAndInitialize());
            }
        }
        JOptionPane.showMessageDialog(null,
                "Placed " + numAgents*linksToPlace.size() + "agents.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    public void setRandom(Random _random) {
        random = _random;
    }
};
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
