// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.vecmath.Vector3d;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.NetworkMapPartsNotifier;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.OBNode.NType;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedAreaPoint;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedAreaRectangle;
import nodagumi.ananPJ.misc.NetMASSnapshot;

import nodagumi.Itk.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

//======================================================================
/**
 * シミュレーションで用いるデータをまとめて保持するクラス。
 */
public class NetworkMap extends NetworkMapBase {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * NetworkParts の prefix の規定値
     */
    public static String DefaultAgentIdPrefix = "ag" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * エージェントテーブル。
     * エージェントの id とエージェントを結びつけるもの。
	 */
    private UniqIdObjectTable<AgentBase> agentTable =
        new UniqIdObjectTable<AgentBase>(DefaultAgentIdPrefix) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * ???
	 */
    private ArrayList<EditorFrame> frames = new ArrayList<EditorFrame>();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * ???
	 */
    private String filename = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * 汚染地域データのファイル
	 */
    private String pollutionFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * エージェント生成ルールのファイル
	 */
    private String generationFile = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
	 * シナリオファイル
	 */
    private String scenarioFile = null;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * fallback file
	 */
	private String fallbackFile = null ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * fallback parameter slot name
	 */
	static public final String FallbackSlot = "_fallback" ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * fallback parameter resource name
	 */
	static public final String FallbackResource = "/fallbackParameters.json" ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * fallback parameter
	 */
	public Term fallbackParameters = null ;

    private boolean hasDisplay = true;
    private Random random = null;
    private NetworkMapPartsNotifier notifier;

    /* undo related stuff */
    class UndoInformation {
        public boolean addition;
        public OBNode parent;
        public OBNode node;

        public UndoInformation(OBNode _parent,
                OBNode _node,
                boolean _addition) {
            parent = _parent;
            node = _node;
            addition = _addition;
        }
    }
    private ArrayList<UndoInformation> undo_list =
        new ArrayList<UndoInformation>();

    public boolean canUndo() {
        return undo_list.size() > 0;

    }
    public void undo(NetworkMapEditor editor) {
        if (undo_list.size() == 0) return;
        int i = undo_list.size() - 1;
        UndoInformation info = undo_list.remove(i);
        if (info.addition) {
            removeOBNode(info.parent, info.node, false);
        } else {
            insertOBNode(info.parent, info.node, false);
        }
        //NetworkMapEditor.getInstance().updateAll();
        editor.updateAll();
    }

    /* constructor */
    public NetworkMap() {
		super() ;
        random = new Random();
        String id = assignNewId();
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
		addObject(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
        notifier = new NetworkMapPartsNotifier(this);
    }

    public NetworkMap(Random _random) {
		super() ;
        random = _random;
        String id = assignNewId();
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
        addObject(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
        notifier = new NetworkMapPartsNotifier(this);
    }

    public NetworkMap(Random _random, boolean _hasDisplay) {
        this(_random);
        setHasDisplay(_hasDisplay);
    }

    /* to restore from DOM */
    public NetworkMap(String id, Random _random) {
		super();
        random = _random;
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
		addObject(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
        notifier = new NetworkMapPartsNotifier(this);
    }

    //------------------------------------------------------------
    /**
     * IDテーブルへのNetworkParts(OBNode)の登録
     * NetworkMapBase での処理に加え、part に NetworkMap 自身を登録する。
     * @param id : part の id.
     * @param part : 登録するpart.
     */
    @Override
    public void addObject(String id, OBNode part) {
        super.addObject(id, part);
        part.setNetworkMap(this);
    }

    //------------------------------------------------------------
    private void insertOBNode (OBNode parent,
                               OBNode node,
                               boolean can_undo) {
        addObject(node.ID, node);
        insertNodeInto(node, parent, parent.getChildCount());

        OBNode.NType type = node.getNodeType();
        if (type == OBNode.NType.NODE) {
            nodesCache.add((MapNode)node);
        } else if (type == OBNode.NType.LINK) {
            linksCache.add((MapLink)node);
        } else if (type == OBNode.NType.AGENT) {
            Itk.logWarn("insert Agent OBNode is obsolute.", node) ;
        } else if (type == OBNode.NType.GROUP) {
            /* no operation */
        } else if (type == OBNode.NType.ROOM) {
            /* no operation */
        } else if (type == OBNode.NType.SYMLINK) {
            /* no operation */
        } else {
            System.err.println("unkown type added");
        }

        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, true));
        }
    }

    abstract class OBTreeCrawlFunctor {
        public abstract void apply(OBNode node,
                OBNode parent); 
    }

    private void applyToAllChildrenRec(OBNode node,
            OBNode parent,
            OBTreeCrawlFunctor func) {
        func.apply(node, parent);
        for (int i = 0; i < node.getChildCount(); ++i) {
            OBNode child = (OBNode)node.getChildAt(i);
            if (child instanceof OBNode) {
                applyToAllChildrenRec(child, node, func);
            }
        }
    }

    public void removeOBNode (OBNode parent,
            OBNode node,
            boolean can_undo) {
        removeObject(node.ID);

        OBNode.NType type = node.getNodeType();
        if (type != OBNode.NType.SYMLINK) {
            clearSymlinks(node);
        }

        boolean linkRemoved = false;
        switch (type) {
        case NODE:
            nodesCache.remove((MapNode)node);
            break;
        case LINK:
            linksCache.remove((MapLink)node);
            linkRemoved = true;
            break;
        case AGENT:
            Itk.logWarn("remove Agent OBNode is obsolute") ;
            break;
        case GROUP:
            while (node.getChildCount() > 0) {
                removeOBNode(node, (OBNode)node.getChildAt(0), true);
            }
            break;
        case SYMLINK:
            break;
        default:
            System.err.println(type);
            System.err.println("unkown type removed");
        }

        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, false));
        }
        removeNodeFromParent(node);

        if (linkRemoved) {
            notifier.linkRemoved((MapLink)node);
        }
    }

    public MapNode createMapNode(
            MapPartGroup parent,
            Point2D _coordinates,
            double _height) {
        String id = assignNewId();
        MapNode node = new MapNode(id,
                _coordinates, _height);
        insertOBNode(parent, node, true);
        return node;
    }

    public MapLink createMapLink(
            MapPartGroup parent,
            MapNode _from,
            MapNode _to,
            double _length,
            double _width) {
        String id = assignNewId();
        MapLink link = new MapLink(id, _from, _to, _length, _width);
        link.prepareAdd();
        insertOBNode(parent, link, true);
        return link;
    }

    //------------------------------------------------------------
    /**
     * エージェントを追加。
     * @param agent : 追加するエージェント。ID は自動生成。
     * @return agent がそのまま変える。
     */
    public AgentBase assignUniqIdForAgent(AgentBase agent) {
        String id = agentTable.putWithUniqId(agent) ;
        agent.ID = id;
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * テーブル中のエージェント総数
     * @return agent 総数
     */
    public int totalAgentNumberInTable() {
        return agentTable.size() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントを追加。
     * @param agent : 追加するエージェント。
     * @param autoID : true なら、ID を自動生成。
     * @return agent がそのまま変える。
     */
    public AgentBase addAgent(AgentBase agent) {
        agent.setNetworkMap(this) ;
        return agent;
    }

    public PollutedArea createPollutedAreaRectangle(
            MapPartGroup parent,
            Rectangle2D bounds,
            double min_height,
            double max_height,
            double angle) {
        String id = assignNewId();
        PollutedArea area = new PollutedAreaRectangle(id,
                bounds, min_height, max_height, angle);
        insertOBNode(parent, area, true);
        return area;
    }

    public PollutedArea createPollutedAreaPoint(
            MapPartGroup parent,
            MapNode node,
            int room_id) {
        String id = assignNewId();
        Vector3d point = node.getPoint();
        PollutedArea area = new PollutedAreaPoint(id, room_id, point); 
        insertOBNode(parent, area, true);
        return area;
    }

    public MapPartGroup createGroupNode(MapPartGroup parent){
        String id = assignNewId();
        MapPartGroup group = new MapPartGroup(id);
        insertOBNode(parent, group, true);
        return group; 
    }

    private MapPartGroup createGroupNode(){
        return createGroupNode((MapPartGroup)root);
    }

    public OBNodeSymbolicLink createSymLink(MapPartGroup parent,
            OBNode orig){
        String id = assignNewId();
        OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id, orig);
        insertOBNode(parent, symlink, true);
        return symlink; 
    }

    public void clearSymlinks(final OBNode orig) {
        applyToAllChildrenRec((OBNode)root,
                null,
                new OBTreeCrawlFunctor() {
                    @Override
                    public void apply(OBNode node, OBNode parent) {
                        if (node.getNodeType() == OBNode.NType.SYMLINK) {
                            OBNodeSymbolicLink symlink =
                                    (OBNodeSymbolicLink)node;
                            if (symlink.getOriginal() == orig) {
                                System.err.println("deleted!");
                                removeOBNode(parent, node, true);
                            }
                        }
                    }
        });
    }

    /* some stuff related to frames */
    public boolean existNodeEditorFrame(MapPartGroup _obiNode){
        for (EditorFrame frame : getFrames()) {
            if (_obiNode.equals(frame)) return true;
        }
        return false;
    }

    //public EditorFrame openEditorFrame(MapPartGroup obinode){
    public EditorFrame openEditorFrame(NetworkMapEditor editor, MapPartGroup obinode) {
        //EditorFrame frame = new EditorFrame(obinode, random);
        EditorFrame frame = new EditorFrame(editor, obinode);

        obinode.setUserObject(frame);

        getFrames().add(frame);
        frame.setVisible(true);

        return frame;
    }

    public void removeEditorFrame(MapPartGroup _obinode){
        getFrames().remove(_obinode.getUserObject());
        _obinode.setUserObject(null);
    }
    
    public ArrayList<MapPartGroup> getGroups() {
        ArrayList<MapPartGroup> groups = new ArrayList<MapPartGroup>();
        for (OBNode node : getOBCollection()) {
            if (node.getNodeType() == OBNode.NType.GROUP &&
                    !node.getTags().isEmpty()) {
                groups.add((MapPartGroup)node);
            }
        }
        Collections.sort(groups, new Comparator<MapPartGroup>() {
            public int compare(MapPartGroup lhs, MapPartGroup rhs) {
                Matcher f1 = lhs.matchTag("((B?)\\d+)F"); 
                Matcher f2 = rhs.matchTag("((B?)\\d+)F");

                if (f1 != null && f2 != null) {
                    int i1;
                    if (f1.group(1).startsWith("B")) {
                        i1 = -Integer.parseInt(f1.group(1).substring(1));
                    } else {
                        i1 = Integer.parseInt(f1.group(1));
                    }
                    int i2;
                    if (f2.group(1).startsWith("B")) {
                        i2 = -Integer.parseInt(f2.group(1).substring(1));
                    } else {
                        i2 = Integer.parseInt(f2.group(1));
                    }

                    return i1 - i2;
                } else if (f1 != null) {
                    return -1;
                } else if (f2 != null) {
                    return 1;
                }

                String tc1Name = lhs.getTagString();
                String tc2Name = rhs.getTagString();
                if (tc1Name.compareTo(tc2Name) < 0) {
                    return -1;
                } else if (tc1Name.compareTo(tc2Name) > 0) {
                    return 1;
                }
                return 0;
            }
        });
        return groups;
    }

    public ArrayList<PollutedArea> getRooms() {
        ArrayList<PollutedArea> rooms = new ArrayList<PollutedArea>();
        findRoomRec((OBNode)root, rooms);
        return rooms;
    }

    private void findRoomRec(OBNode node, ArrayList<PollutedArea> rooms) {
        if (node.getNodeType() == NType.ROOM) {
            rooms.add((PollutedArea)node);
        }
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) findRoomRec((OBNode)child, rooms);
        }
    }

    public void makeStairs() {
        MapNodeTable selected_nodes = new MapNodeTable();
        for (MapNode node : getNodes()) {
            if (node.selected) selected_nodes.add(node);
        }
        if (selected_nodes.size() != 2) {
            JOptionPane.showMessageDialog(null,
                    "Number of selected nodes:"
                    + selected_nodes.size() + "\nwhere it should be 2",
                    "Fail to make stair",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        MapNode from_node = selected_nodes.get(0);
        MapNode to_node = selected_nodes.get(1);

        if (from_node.getHeight() < to_node.getHeight()) {
            from_node = selected_nodes.get(1);
            to_node = selected_nodes.get(0);
        }
        MapLink link = createMapLink((MapPartGroup)from_node.getParent(),
                from_node, to_node,    100, 6);
        link.addTag("GENERATED_STAIR");

    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String file_name) {
        filename = file_name;
    }

    public String getPollutionFile() {
        if (pollutionFile == null)
            return null;
        File pollution_file = new File(pollutionFile);
        // if (!pollution_file.isAbsolute()) {
            // File mapfile = new File(filename);
            // pollution_file = new File(mapfile.getParent()
                          // + File.separator
                          // + pollutionFile);
        // }
        // return pollution_file.getAbsolutePath();
        return pollution_file.getPath();
    }

    public void setPollutionFile(String s) {
        pollutionFile = s;
    }

    public String getGenerationFile() {
        if (generationFile == null)
            return null;
        File generation_file = new File(generationFile);
        // if (!generation_file.isAbsolute() && (filename != null)) {
            // File mapfile = new File(filename);
            // if (mapfile.getParent() != null) {
        // generation_file = new File(mapfile.getParent()
                       // + File.separator
                       // + generationFile);
            // } else {
        // return generationFile;
        // }
        // }
        // return generation_file.getAbsolutePath();
        return generation_file.getPath();
    }

    public void setGenerationFile(String s) {
        generationFile = s;
    }

	public String getScenarioFile() {
        if (scenarioFile == null)
            return null;
		File _scenarioFile = new File(scenarioFile) ;
        return _scenarioFile.getPath();
    }

    public void setScenarioFile(String s) {
        scenarioFile = s;
    }

	//------------------------------------------------------------
	/**
	 * fallback （デフォルトセッティング）のファイル取得
	 */
	public String getFallbackFile() {
		return fallbackFile ;
	}

	//------------------------------------------------------------
	/**
	 * fallback （デフォルトセッティング）のファイルセット
	 */
	public void setFallbackFile(String s) {
		fallbackFile = s ;
	}

	//------------------------------------------------------------
	/**
	 * fallback （デフォルトセッティング）の読み込み
	 * @param scanResourceP : resource の fallback も読み込むかどうか
	 */
	public void scanFallbackFile(boolean scanResourceP) {
		if(fallbackFile != null) {
			try {
				BufferedReader buffer =
					new BufferedReader(new FileReader(fallbackFile)) ;
				fallbackParameters = Term.newByScannedJson(JSON.decode(buffer),
														   true) ;
				Itk.logInfo("Load Fallback File", fallbackFile) ;
			} catch (Exception ex) {
				ex.printStackTrace() ;
				Itk.logError("Can not scan a fallback parameter file:",
							 fallbackFile) ;
				Itk.logError_("Exception",ex) ;
			}
		} else {
			fallbackParameters = new Term() ;
		}

		if(scanResourceP) {
			try {
				InputStream istrm =
					getClass().getResourceAsStream(FallbackResource) ;
				Term finalFallback =
					Term.newByScannedJson(JSON.decode(istrm),true) ;
				fallbackParameters.setArg(FallbackSlot, finalFallback) ;
			} catch (Exception ex) {
				ex.printStackTrace() ;
				Itk.logError("Can not scan a fallback resource file.") ;
				Itk.logError_("Exception",ex) ;
			}
		}
	}

    /* converting to/from DOM */
    public boolean toDOM(Document doc) {
        Element dom_root =((OBNode)this.root).toDom(doc, "root");

        doc.appendChild(dom_root);

        return true;
    }

    public boolean fromDOM(Document doc) {
        NodeList toplevel = doc.getChildNodes();
        if (toplevel.getLength() != 1) {
            JOptionPane.showMessageDialog(null,
                    "The number of networks in the dom was "
                    + toplevel.getLength(),
                    "Fail to convert from DOM",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Element dom_root = (Element) toplevel.item(0);

        setRoot(OBNode.fromDom(dom_root));
        setupNetwork((OBNode)this.root);
        return true;
    }

    private void setupNetwork(OBNode ob_node) {
        setupNodes(ob_node);
        setupLinks(ob_node);
        setupOthers(ob_node);

        checkDanglingSymlinks(ob_node);
    }

    @SuppressWarnings("unchecked")
    private void setupNodes(OBNode ob_node) {
        if (OBNode.NType.NODE == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapNode node = (MapNode) ob_node;

            nodesCache.add(node);
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupNodes(child);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupLinks(OBNode ob_node) {
        if (OBNode.NType.LINK == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            MapLink link = (MapLink) ob_node;
            linksCache.add(link);

            String[] nodes = (String[])ob_node.getUserObject();
            MapNode from_node = (MapNode)getObject(nodes[0]);
            MapNode to_node = (MapNode)getObject(nodes[1]);

            if (from_node == null) {
                System.err.println(Integer.parseInt("from_node is null " + nodes[0]));
            }
            if (to_node == null) {
                System.err.println(Integer.parseInt("to_node is null " + nodes[1]));
            }
            from_node.addLink(link);
            to_node.addLink(link);
            try {
                link.setFromTo(from_node, to_node);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Try to set from/to of a link, which alread has it setted\n"
                        + "link ID: " + link.ID,
                        "Warning setting up network",
                        JOptionPane.WARNING_MESSAGE);
            }
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupLinks(child);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setupOthers(OBNode ob_node) {
        if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            addObject(ob_node.ID, ob_node);
            for (Enumeration<OBNode> e = ob_node.children(); e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupOthers(child);
            }
        } else if (ob_node.getNodeType() == OBNode.NType.SYMLINK) {
            addObject(ob_node.ID, ob_node);
            String orig_id = (String)ob_node.getUserObject();
            OBNode original = (OBNode)getObject(orig_id);
            
            OBNodeSymbolicLink symlink = (OBNodeSymbolicLink)ob_node;
            symlink.setOriginal(original);
        } else if (ob_node.getNodeType() == OBNode.NType.ROOM){
            addObject(ob_node.ID, ob_node);
        } else if (ob_node.getNodeType() == OBNode.NType.NODE ||
                ob_node.getNodeType() == OBNode.NType.LINK ||
                ob_node.getNodeType() == OBNode.NType.AGENT
                ){
        } else {
            System.err.println("unknown type " + ob_node.getNodeType() + " in setting up network");
        }
    }

    private void checkDanglingSymlinks(OBNode node) {
        class CheckSymlink extends OBTreeCrawlFunctor {
            public int dangling_symlink_count = 0;
            boolean found = false;
            @Override
            public void apply(OBNode node,
                    OBNode parent) {
                if (node.getNodeType() == OBNode.NType.SYMLINK) {
                    OBNode original = ((OBNodeSymbolicLink)node).getOriginal();
                    if (original == null) {
                        dangling_symlink_count++;
                        found = true;
                        removeOBNode(parent, node, false);
                    }
                }
            }

            public boolean mustCheckMore() {
                boolean ret = found;
                found = false;
                return ret;
            }
        }
        CheckSymlink checker = new CheckSymlink();
        do {
            applyToAllChildrenRec(node, null, checker);
        } while(checker.mustCheckMore());
        if (checker.dangling_symlink_count != 0) {
            JOptionPane.showMessageDialog(null,
                    "Removed " + checker.dangling_symlink_count
                    + " dangling symlink(s) on loading",
                    "Corrupt file",
                    JOptionPane.WARNING_MESSAGE
                    );
        }

    }

    public void setFrames(ArrayList<EditorFrame> frames) {
        this.frames = frames;
    }
    public ArrayList<EditorFrame> getFrames() {
        return frames;
    }
    
    public ArrayList<String> getAllTags() {
        ArrayList<String> all_tags = new ArrayList<String>();
        for (MapNode node : getNodes()) {
            for (String tag : node.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        for (MapLink link : getLinks()) {
            for (String tag : link.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        return all_tags;
    }
    
    private void prepare_for_save_rec(OBNode node) {
        node.prepareForSave(hasDisplay);
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) prepare_for_save_rec((OBNode)child);
        }
    }
    public void prepareForSave() {
        prepare_for_save_rec((OBNode)root);
    }
    private void setup_after_load_rec(OBNode node) {
        node.postLoad(hasDisplay);
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) setup_after_load_rec((OBNode)child);
        }
    }
    public void setupAfterLoad() {
        setup_after_load_rec((OBNode)root);
    }

    public void setHasDisplay(boolean _hasDisplay) {
        hasDisplay = _hasDisplay;
    }

    public boolean getHasDisplay() {
        return hasDisplay;
    }

    public void setRandom(Random _random) {
        random = _random;
        for (AgentBase agent : agentTable.values())
            agent.setRandom(_random);
    }

    /**
     * NetworkMap の構成要素の状態変化を監視・通知するオブジェクトを返す.
     */
    public NetworkMapPartsNotifier getNotifier() { return notifier; }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
