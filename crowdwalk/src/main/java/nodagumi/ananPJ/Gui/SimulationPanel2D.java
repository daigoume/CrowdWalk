// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

import org.apache.batik.ext.awt.geom.Polygon2D;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.LinkAppearance;
import nodagumi.ananPJ.Gui.NodeAppearance;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.Hover;
import nodagumi.ananPJ.Simulator.SimulationPanel3D.gas_display;

/**
 * 2D シミュレーションパネル
 */
public class SimulationPanel2D extends JPanel {
    /**
     * マップをセンタリング表示する際に入れるマージンのドット数
     */
    public static final int CENTERING_MARGIN = 30;

    /**
     * ステータスメッセージの背景色
     */
    public static final Color BACKGROUND_COLOR = new Color(0.95f, 0.95f, 0.95f);

    /**
     * ラベル表示に使用するフォント名
     */
    public static final String FONT_NAME = "Consolas";

    /**
     * テキスト描画の際に指定する描画位置
     */
    public static enum TextPosition { UPPER_CENTER, LOWER_CENTER, UPPER_RIGHT, LOWER_RIGHT }

    /**
     * シミュレーションウィンドウのフレーム
     */
    private SimulationFrame2D frame = null;

    /**
     * AIST ロゴ画像
     */
    private Image aistLogo = getToolkit().createImage(getClass().getResource("/img/aist_logo.png"));

    /**
     * 描画前に平行移動する距離
     */
    private double tx = 0.0, ty = 0.0;

    /**
     * 描画スケール
     */
    private double scale = 1.0;

    /**
     * ASCIIキャラクタフォントのデフォルトスケール時の文字幅
     */
    private int[] originalCharsWidths;

    /**
     * フォントのデフォルトスケール時の文字高さ
     */
    private double originalTextHeight = 0.0;

    /**
     * フォントのデフォルトスケール時のascent
     */
    private double originalAscent = 0.0;

    /**
     * フォントのデフォルトスケール時のdescent
     */
    private double originalDescent = 0.0;

    /**
     * フォントのデフォルトスケール時のleading
     */
    private double originalLeading = 0.0;

    /**
     * フォントの現在のスケールでの高さ
     */
    private double fontHeight = 0.0;

    /**
     * フォントの現在のスケールでのascent
     */
    private double fontAscent = 0.0;

    /**
     * フォントの現在のスケールでのdescent
     */
    private double fontDescent = 0.0;

    /**
     * フォントの現在のスケールでのleading
     */
    private double fontLeading = 0.0;

    /* ホバー表示対象 */

    private MapNode hoverNode = null;
    private MapLink hoverLink = null;
    private MapArea hoverArea = null;
    private AgentBase hoverAgent = null;

    /* 表示 ON/OFF フラグ */

    private boolean showNodes = true;
    private boolean showNodeLabels = false;
    private boolean showLinks = true;
    private boolean showLinkLabels = false;
    private boolean showArea = true;
    private boolean showAreaLabels = false;
    private boolean showAgents = true;
    private boolean showAgentLabels = false;

    /**
     * 通常リンクの表示色
     */
    private Color defaultLinkColor = new Color(0.1f, 0.1f, 0.1f, 1.0f - 0.75f);

    /**
     * エリアの表示色
     */
    private gas_display show_gas = gas_display.ORANGE;

    /**
     * ホバーテキストの背景色
     */
    private Color hoverBgColor = new Color(0.8f, 0.8f, 0.9f);

    /**
     * エリア表示色の彩度 100% に相当する Obstructer level
     */
    private double pollutionColorSaturation = 0.0;

    /**
     * 地図データ。
     */
    private NetworkMap networkMap;

    /**
     * ポリゴンを除いたリンクのリスト
     */
    private MapLinkTable regularLinks = new MapLinkTable();

    /**
     * タグ別ポリゴンオブジェクト
     */
    private HashMap<String, Polygon2D> polygons = new HashMap();

    /**
     * タグ別リンク表示スタイル
     */
    private LinkedHashMap<String, LinkAppearance> linkAppearances = new LinkedHashMap();

    /**
     * タグ別ノード表示スタイル
     */
    private LinkedHashMap<String, NodeAppearance> nodeAppearances = new LinkedHashMap();

    /**
     * 表示更新済みフラグ
     */
    private boolean updated = false;

    synchronized public boolean isUpdated() {
        return updated;
    }

    synchronized public void setUpdated(boolean b) {
        updated = b;
    }

    /* constructor
     */
    public SimulationPanel2D(SimulationFrame2D _frame, NetworkMap _networkMap, CrowdWalkPropertiesHandler properties) {
        super();
        frame = _frame;
        networkMap = _networkMap;

        if (properties != null) {
            try {
                String filePath = null;
                if (properties.isDefined("link_appearance_file")) {
                    filePath = properties.getFilePath("link_appearance_file", null);
                    LinkAppearance.loadLinkAppearances(new FileInputStream(filePath), linkAppearances);
                }
                LinkAppearance.loadLinkAppearances(
                        getClass().getResourceAsStream("/link_appearance.json"), linkAppearances);
                // 該当するタグが複数存在した場合には、設定ファイルの記述が上にある方を採用する。
                // このルールに従わせるため再ロードが必要。
                if (properties.isDefined("link_appearance_file")) {
                    LinkAppearance.loadLinkAppearances(new FileInputStream(filePath), linkAppearances);
                }

                if (properties.isDefined("node_appearance_file")) {
                    filePath = properties.getFilePath("node_appearance_file", null);
                    NodeAppearance.loadNodeAppearances(new FileInputStream(filePath), nodeAppearances);
                }
                NodeAppearance.loadNodeAppearances(
                        getClass().getResourceAsStream("/node_appearance.json"), nodeAppearances);
                if (properties.isDefined("node_appearance_file")) {
                    NodeAppearance.loadNodeAppearances(new FileInputStream(filePath), nodeAppearances);
                }

                show_gas = gas_display.valueOf(properties.getString("pollution_color", "ORANGE",
                            gas_display.getNames()).toUpperCase()) ;
                pollutionColorSaturation = properties.getDouble("pollution_color_saturation", 0.0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        polygons = createPolygons(networkMap.getLinks());
        // ポリゴンを除いたリンクのリスト
        for (MapLink link : frame.getLinks()) {
            if (! link.containsTag("POLYGON")) {
                regularLinks.add(link);
            }
        }
    }

    /**
     * リンクリストからポリゴンデータを生成する
     */
    public HashMap<String, Polygon2D> createPolygons(MapLinkTable links) {
        // タグごとにリンクを選別する
        HashMap<String, MapLinkTable> polygonLinks = new HashMap();
        for (MapLink link : links) {
            for (String tag : link.getTags()) {
                if (tag.contains("POLYGON")) {
                    MapLinkTable _links = polygonLinks.get(tag);
                    if (_links == null) {
                        _links = new MapLinkTable();
                        polygonLinks.put(tag, _links);
                    }
                    _links.add(link);
                }
            }
        }
        // 選別したリンクでポリゴンを生成する
        HashMap<String, Polygon2D> polygons = new HashMap();
        for (String tag : polygonLinks.keySet()) {
            // 通過点順に MapNode を収集する
            MapLinkTable _links = polygonLinks.get(tag);
            MapLink currentLink = _links.get(0);
            MapNode start = currentLink.getFrom();
            MapNode next = currentLink.getTo();
            MapNodeTable polygonNodes = new MapNodeTable();
            polygonNodes.add(start);
            while (next != start) {
                polygonNodes.add(next);
                for (MapLink link : _links) {
                    if (link == currentLink) {
                        continue;
                    }
                    if (next == link.getFrom() || next == link.getTo()) {
                        currentLink = link;
                        if (currentLink.getFrom() != next)
                            next = currentLink.getFrom();
                        else
                            next = currentLink.getTo();
                        break;
                    }
                }
            }
            // 収集した MapNode でポリゴンオブジェクトを生成する
            Polygon2D polygon = new Polygon2D();
            for (MapNode node : polygonNodes) {
                polygon.addPoint((float)node.getX(), (float)node.getY());
            }
            polygons.put(tag, polygon);
        }
        return polygons;
    }

    /**
     * マウスカーソル位置を基準にしてズームイン/ズームアウトする.
     */
    public void zoom(int z) {
        double scaleOld = scale;

        double r;
        if (z > 0) {
            r = 0.8;
        } else {
            r = 1.25;
        }

        z = Math.abs(z);
        for (int i = 0; i < z; i++) {
            scale *= r;
        }
        
        if (scale > 100.0) scale = 100.0;
        if (scale < 0.01) scale = 0.01;

        //TODO: needs calculation for rotation
        tx += frame.mousePoint.getX() * (scaleOld - scale);
        ty += frame.mousePoint.getY() * (scaleOld - scale);
    }
    
    /**
     * 描画スケールを返す
     */
    public double getDrawingScale() {
        return scale;
    }
    
    /**
     * マップをスクロールする
     */
    public void scroll(int dx, int dy) {
        tx += dx;
        ty += dy;
    }

    /**
     * 描画前に平行移動する距離を指定する
     */
    public void setPosition(int dx, int dy) {
        tx = dx;
        ty = dy;
    }

    /**
     * マップをセンタリングする
     */
    public void centering(boolean withScaling) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        // ※Double.MIN_VALUE が何故か正の値と判定されることがあるため Long.MIN_VALUE で代用した
        double maxX = (double)Long.MIN_VALUE, maxY = (double)Long.MIN_VALUE;
        for (MapNode node : networkMap.getNodes()) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY());
        }
        if (minX == Double.MAX_VALUE) {
            tx = 0.0;
            ty = 0.0;
            scale = 1.0;
            return;
        }

        if (withScaling) {
            double width = maxX - minX;
            double height = maxY - minY;
            double scaleX, scaleY;
            if (frame.isMarginAdded()) {
                scaleX = (getWidth() - CENTERING_MARGIN * 2) / width;
                scaleY = (getHeight() - CENTERING_MARGIN * 2) / height;
            } else {
                scaleX = getWidth() / width;
                scaleY = getHeight() / height;
            }
            scale = Math.min(scaleX, scaleY);
        }

        tx = -(minX + maxX) / (2) * scale + getWidth() / 2;
        ty = -(minY + maxY) / (2) * scale + getHeight() / 2;
    }

    /* display to theoretical value */
    /* this method is called by zoom relate function */  
    public Point2D revCalcPos(int x, int y) {
        Point point_on_panel = SwingUtilities.convertPoint(null, x, y, this);
        
        AffineTransform trans = new AffineTransform();
        trans.translate(tx, ty);
        trans.scale(scale, scale);

        try {
            trans.invert();
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }
        return trans.transform(new Point2D.Double(point_on_panel.getX(), point_on_panel.getY()), null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // viewArea を使って表示範囲外の無駄な描画処理を省く
        Dimension frameSize = new Dimension();
        frame.getSize(frameSize);
        Point2D position = revCalcPos(0, 0);
        Point2D size = revCalcPos(frameSize.width, frameSize.height);
        Rectangle2D viewArea = new Rectangle2D.Double(position.getX(), position.getY(), (size.getX() - position.getX()) + 1, (size.getY() - position.getY()) + 1);

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        AffineTransform saveAT = g2.getTransform();

        if (originalTextHeight == 0.0) {
            g2.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            originalTextHeight = (double)fm.getHeight();
            originalAscent = (double)fm.getAscent();
            originalDescent = (double)fm.getDescent();
            originalLeading = (double)fm.getLeading();
            originalCharsWidths = fm.getWidths();
        }
        fontHeight = originalTextHeight / scale;
        fontAscent = originalAscent / scale;
        fontDescent = originalDescent / scale;
        fontLeading = originalLeading / scale;

        g2.translate(tx, ty);
        g2.scale(scale, scale);
        setScaleFixedFont(g2, FONT_NAME, Font.PLAIN, 14);

        // ポリゴンの描画
        if (! polygons.isEmpty()) {
            for (String tag : polygons.keySet()) {
                Polygon2D polygon = polygons.get(tag);
                drawPolygon(g2, tag, polygon);
            }
        }

        /* actual objects */
        if (showArea) {
            for (MapArea area : frame.getMapAreas()) {
                drawArea(area, g2, showAreaLabels) ;
            }
        }
        if (showLinks) {
            for (MapLink link : regularLinks) {
                if (viewArea.intersectsLine(link.getLine2D())) {
                    drawLink(link, g2, showLinkLabels, false);
                }
            }
        }
        if (showNodes) {
            g2.setStroke(new BasicStroke((float)(1.0 / scale)));
            for (MapNode node : frame.getNodes()) {
                if (viewArea.contains(node.getX(), node.getY())) {
                    drawNode(node, g2, showNodeLabels, false);
                }
            }
        }
        if (showAgents) {
            for (AgentBase agent : frame.getWalkingAgents()) {
                if (! agent.isEvacuated()) {
                    drawAgent(agent, g2, showAgentLabels);
                }
            }
        }

        /* symbolic links */
        // TODO: 後日アップデートにてサポートする

        /* temporary objects */
        if (hoverNode != null) {
            drawHoverNode(hoverNode, g2);
        }
        if (hoverLink != null) {
            drawHoverLink(hoverLink, g2);
        }
        if (hoverArea != null) {
            drawHoverArea(hoverArea, g2);
        }
        if (hoverAgent != null) {
            drawHoverAgent(hoverAgent, g2);
        }

        // アフィン変換をリセットする
        g2.setTransform(saveAT);

        if (frame.isShowLogo()) {
            drawLogo(g2);
        }

        if (frame.isShowStatus()) {
            drawMessage(g2, frame.getStatusPosition(), frame.getStatusText());
        }

        // 表示更新待ちを解除する
        if (frame.isViewSynchronized()) {
            synchronized (this) {
                setUpdated(true);
                notify();
            }
        }
    }

    /**
     * エージェントを描画する
     */
    public void drawAgent(AgentBase agent, Graphics2D g2, boolean showLabel) {
        double scale = g2.getTransform().getScaleX();
        Point2D pos = agent.getPos();
        Vector3d swing = agent.getSwing();
        double size = frame.getAgentSize() / scale;

        if (showLabel) {
            String text = agent.getIdNumber();
            double cx = pos.getX() + swing.getX();
            double cy = pos.getY() + swing.getY() + size / 2.0;
            g2.setColor(Color.RED);
            drawText(g2, cx, cy, TextPosition.LOWER_CENTER, text);
        }

        g2.setColor(getAgentColor(agent));
        double x = pos.getX() + swing.getX() - size / 2.0;
        double y = pos.getY() + swing.getY() - size / 2.0;
        g2.fill(new Ellipse2D.Double(x, y, size, size));
    }

    /**
     * エージェントのホバーを描画する
     */
    public void drawHoverAgent(AgentBase agent, Graphics2D g2) {
        double scale = g2.getTransform().getScaleX();
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.setColor(Color.BLUE);
        Point2D pos = agent.getPos();
        Vector3d swing = agent.getSwing();
        double diameter = 8.0 / scale;
        double size = frame.getAgentSize() / scale;

        String text = agent.getIdNumber();
        double cx = pos.getX() + swing.getX();
        double cy = pos.getY() + swing.getY() + size / 2.0;
        drawText(g2, cx, cy, TextPosition.LOWER_CENTER, text, hoverBgColor);

        double x = pos.getX() + swing.getX() - diameter / 2.0;
        double y = pos.getY() + swing.getY() - diameter / 2.0;
        g2.draw(new Ellipse2D.Double(x, y, diameter, diameter));
    }
    
    /**
     * エージェントの移動速度に応じた表示色を返す
     */
    public Color speedToColor(double speed) {
	float f = ((float) Math.pow(speed,5)) * 0.35f;
        return new Color(Color.HSBtoRGB(f, 0.8588f, 0.698f));
    }

    /**
     * エージェントの表示色を返す
     */
    public Color getAgentColor(AgentBase agent) {
        Color3f color = Colors.DEFAULT_AGENT_COLOR;

        switch (agent.getTriage()) {
        case GREEN:
            if (frame.getChangeAgentColorDependingOnSpeed()) {
                return speedToColor(agent.getSpeed());
            } else if (agent.hasTag("BLUE")){
                color = Colors.BLUE;
            } else if (agent.hasTag("APINK")){
                color = Colors.APINK;
            } else if (agent.hasTag("YELLOW")){
                color = Colors.YELLOW;
            }
            break;
        case YELLOW:
            color = Colors.YELLOW;
            break;
        case RED:
            color = Colors.PRED;
            break;
        case BLACK:
            color = Colors.BLACK2;
            break;
        }
        return new Color(color.x, color.y, color.z);
    }

    /**
     * ポリゴンを描画する
     */
    public void drawPolygon(Graphics2D g2, String tag, Polygon2D polygon) {
        Color3f color = Colors.GRAY;
        if (tag.contains("OCEAN")) {
            color = Colors.SLATEBLUE;
        } else if (tag.contains("STRUCTURE")) {
            color = Colors.LIGHTGRAY;
        }
        g2.setColor(new Color(color.x, color.y, color.z));
        g2.fill(polygon);

        // ポリゴン間に隙間が出来てしまうのを防ぐ
        double scale = g2.getTransform().getScaleX();
        g2.setStroke(new BasicStroke((float)(2.0 / scale)));
        g2.draw(polygon);
    }

    /**
     * ノードを描画する
     */
    public void drawNode(MapNode node, Graphics2D g, boolean showLabel, boolean isSymbolic) {
        double diameter = 0.0;
        NodeAppearance nodeAppearance = getNodeAppearance(node);
        if (nodeAppearance != null) {
            g.setColor(nodeAppearance.awtColor);
            diameter = nodeAppearance.diameter;
            double x = node.getX() - diameter / 2.0;
            double y = node.getY() - diameter / 2.0;
            g.fill(new Ellipse2D.Double(x, y, diameter, diameter));
        }

        if (showLabel) {
            String text = node.getHintString();
            if (! text.isEmpty()) {
                double cx = node.getX();
                double cy = node.getY() - diameter / 2.0;
                g.setColor(Color.darkGray);
                drawText(g, cx, cy, TextPosition.UPPER_CENTER, text);
            }
        }
    }

    /**
     * ノードのホバーを描画する
     */
    public void drawHoverNode(MapNode node, Graphics2D g2) {
        double diameter = 0.0;
        NodeAppearance nodeAppearance = getNodeAppearance(node);
        if (nodeAppearance != null) {
            diameter = nodeAppearance.diameter;
        }

        g2.setColor(Color.BLUE);
        String text = node.getHintString();
        if (! text.isEmpty()) {
            double cx = node.getX();
            double cy = node.getY() - diameter / 2.0;
            drawText(g2, cx, cy, TextPosition.UPPER_CENTER, text, hoverBgColor);
        }

        double scale = g2.getTransform().getScaleX();
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        diameter = 8.0 / scale;
        double x = node.getX() - diameter / 2.0;
        double y = node.getY() - diameter / 2.0;
        g2.draw(new Ellipse2D.Double(x, y, diameter, diameter));
    }
    
    /**
     * リンクを描画する
     */
    public void drawLink(MapLink link, Graphics2D g, boolean show_label, boolean isSymbolic) {
        double scale = g.getTransform().getScaleX();
        float width = 1.0f;
        Color color = defaultLinkColor;
        LinkAppearance linkAppearance = getLinkAppearance(link);
        if (linkAppearance != null) {
            width = (float)(linkAppearance.widthFixed ?
                    linkAppearance.widthRatio : link.getWidth() * linkAppearance.widthRatio);
            color = linkAppearance.awtColor;
        } else {
            width /= scale;
        }
        g.setStroke(new BasicStroke((float)width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(color);
        g.draw(link.getLine2D());

        if (show_label) {
            String text = link.getTagString();
            if (! text.isEmpty()) {
                Point2D middlePoint = link.getMiddlePoint();
                double cx = middlePoint.getX();
                double cy = middlePoint.getY() - width / scale / 2.0;
                g.setColor(Color.darkGray);
                drawText(g, cx, cy, TextPosition.UPPER_CENTER, text);
            }
        }
    }

    /**
     * リンクのホバーを描画する
     */
    public void drawHoverLink(MapLink link, Graphics2D g2) {
        double scale = g2.getTransform().getScaleX();
        g2.setColor(Color.BLUE);
        g2.fill(link.getRect(0.0, scale, false));

        String text = link.getTagString();
        if (! text.isEmpty()) {
            float width = 1.0f;
            LinkAppearance linkAppearance = getLinkAppearance(link);
            if (linkAppearance != null) {
                width = (float)(linkAppearance.widthFixed ?
                        linkAppearance.widthRatio : link.getWidth() * linkAppearance.widthRatio);
            }
            Point2D middlePoint = link.getMiddlePoint();
            double cx = middlePoint.getX();
            double cy = middlePoint.getY() - width / scale / 2.0;
            g2.setColor(Color.darkGray);
            drawText(g2, cx, cy, TextPosition.UPPER_CENTER, text, hoverBgColor);
        }
    }

    /**
     * エリアを描画する
     */
    public void drawArea(MapArea area, Graphics2D g, boolean showLabel) {
        if (area.getPollutionLevel() == null || ! area.getPollutionLevel().isPolluted()) {
            if (frame.isShowAreaLocation()) {
                g.setColor(Color.WHITE);
                g.fill(area.getShape());
            }
            return;
        }

        float density = 0.0f;
        if (pollutionColorSaturation == 0.0) {
            density = (float)area.getPollutionLevel().getNormalizedLevel();
        } else {
            density = (float)(area.getPollutionLevel().getCurrentLevel() / pollutionColorSaturation);
            if (density > 1.0f)
                density = 1.0f;
        }
        float alpha = density / 1.5f;

        Color color = null;
        switch (show_gas) {
        case RED:
            color = new Color(density, 0.0f, 0.0f, alpha);
            break;
        case BLUE:
            color = new Color(0.0f, 0.0f, density, alpha);
            break;
        case HSV:
            float f = (1.0f - density) * 0.65f;
            color = new Color(Color.HSBtoRGB(f, 1.0f, 1.0f));
            break;
        case ORANGE:
            color = new Color(1.0f, 1.0f - density / 2.0f, 0.0f, alpha);
            break;
        }

        g.setColor(color);
        g.fill(area.getShape());

        if (showLabel) {
            String text = area.getTagString();
            if (! text.isEmpty()) {
                g.setColor(Color.darkGray);
                double cx = ((Rectangle2D)area.getShape()).getMinX();
                double cy = ((Rectangle2D)area.getShape()).getMinY();
                // TODO: バックグラウンドカラーが何故か微妙に異なってしまう
                drawText(g, cx, cy, TextPosition.LOWER_RIGHT, text, color);
            }
        }
    }

    /**
     * エリアのホバーを描画する
     */
    public void drawHoverArea(MapArea hoverArea, Graphics2D g2) {
        double scale = g2.getTransform().getScaleX();
        g2.setStroke(new BasicStroke((float)(3.0 / scale)));
        g2.setColor(Color.BLUE);
        g2.draw(hoverArea.getShape());

        String text = hoverArea.getTagString();
        if (! text.isEmpty()) {
            double cx = ((Rectangle2D)hoverArea.getShape()).getMinX();
            double cy = ((Rectangle2D)hoverArea.getShape()).getMinY();
            drawText(g2, cx, cy, TextPosition.LOWER_RIGHT, text, hoverBgColor);
        }
    }

    /**
     * ステータスメッセージを描画する
     */
    public void drawMessage(Graphics2D g2, int position, String message) {
        Font font = new Font(FONT_NAME, Font.PLAIN, 14);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(message);
        int height = fm.getHeight();
        int ascent = fm.getAscent();
        int x = 12;     // メッセージの基準表示位置
        int y = 12;     //          〃
        if ((position & frame.BOTTOM) == frame.BOTTOM) {
            y += getHeight() - ascent;
        }
        g2.setColor(BACKGROUND_COLOR);           // メッセージの背景色
        g2.fillRect(x - 4, y - ascent, width + 7, height - 1);  // メッセージの背景描画
        g2.setColor(Color.BLACK);
        g2.drawString(message, x, y);
    }

    /**
     * AIST ロゴを描画する
     */
    public void drawLogo(Graphics2D g2) {
        int x = getWidth() - aistLogo.getWidth(null);
        int y = getHeight() - aistLogo.getHeight(null);
        g2.drawImage(aistLogo, x, y, this);
    }

    /**
     * 描画スケールの影響を受けないフォントの指定
     */
    public void setScaleFixedFont(Graphics2D g2, String name, int fontStyle, int fontSize) {
        double scale = g2.getTransform().getScaleX();
        int size = (int)(fontSize / scale);
        Font font = new Font(name, fontStyle, fontSize);
        g2.setFont(font.deriveFont((float)(fontSize / scale)));
    }

    /**
     * FontMetrics.stringWidth(String str) の代用
     */
    public int getStringWidth(String text) {
        int size = 0;
        for (byte c : text.getBytes()) {
            size += originalCharsWidths[(int)c];
        }
        return size;
    }

    /**
     * 指定された位置に text を描画する。
     */
    public void drawText(Graphics2D g2, double x, double y, TextPosition position, String text, Color bgColor) {
        double scale = g2.getTransform().getScaleX();
        double textWidth = getStringWidth(text) / scale;

        double dx = 0.0;
        double dy = 0.0;
        switch (position) {
        case UPPER_CENTER:
            dx = textWidth / -2.0;
            dy = -(fontDescent + fontLeading);
            break;
        case LOWER_CENTER:
            dx = textWidth / -2.0;
            dy = fontAscent;
            break;
        case UPPER_RIGHT:
            dx = fontHeight / 3.0;
            dy = -(fontDescent + fontLeading);
            break;
        case LOWER_RIGHT:
            dx = fontHeight / 3.0;
            dy = fontAscent;
            break;
        }

        Color color = g2.getColor();
        g2.setColor(bgColor);
        g2.fill(new Rectangle2D.Double(x + dx - fontHeight / 3.0, y + dy - fontAscent,
                    textWidth + fontHeight * 2.0 / 3.0, fontHeight));
        g2.setColor(color);
        g2.drawString(text, (float)(x + dx), (float)(y + dy));
    }

    /**
     * 指定された位置に text を描画する。
     */
    public void drawText(Graphics2D g2, double x, double y, TextPosition position, String text) {
        drawText(g2, x, y, position, text, getBackground());
    }

    /**
     * node に振られているタグにマッチする NodeAppearance を返す.
     */
    public NodeAppearance getNodeAppearance(MapNode node) {
        return NodeAppearance.getAppearance(nodeAppearances, node);
    }

    /**
     * link に振られているタグにマッチする LinkAppearance を返す.
     */
    public LinkAppearance getLinkAppearance(MapLink link) {
        return LinkAppearance.getAppearance(linkAppearances, link);
    }

    /* -- Methods to set how drawn 
     */
    public void updateHoverNode(MapNode _hoverNode) {
        hoverNode = _hoverNode;
    }
    
    public void updateHoverLink(MapLink _hoverLink) {
        hoverLink = _hoverLink;
    }
    
    public void updateHoverArea(MapArea area) {
        hoverArea = area;
    }
    
    public void updateHoverAgent(AgentBase agent) {
        hoverAgent = agent;
    }
    
    public void setShowNodes(boolean showNodes) {
        this.showNodes = showNodes;
    }

    public void setShowNodeNames(boolean showNodeNames) {
        this.showNodeLabels = showNodeNames;
    }
    
    public void setShowLinkNames(boolean showLinkNames) {
        this.showLinkLabels = showLinkNames;
    }

    public void setShowLinks(boolean showLinks) {
        this.showLinks = showLinks;
    }

    public void setShowArea(boolean showArea) {
        this.showArea = showArea;
    }

    public void setShowAreaNames(boolean showAreaNames) {
        this.showAreaLabels = showAreaNames;
    }

    public void setShowAgents(boolean showAgents) {
        this.showAgents = showAgents;
    }

    public void setShowAgentNames(boolean showAgentNames) {
        this.showAgentLabels = showAgentNames;
    }
}
