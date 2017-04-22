// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Area;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JTextField;

import math.geom3d.Point3D;

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.Simulator.PollutionHandler.*;

//======================================================================
/**
 * 地図上のエリア。
 */
public abstract class MapArea extends OBNode {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在の Pollution 情報
     */
    public PollutionLevelInfo pollutionLevel = null ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public MapArea(String _id) {
        super(_id);
    }

    //------------------------------------------------------------
    /**
     * Pollution Level を格納。
     */
    public void pollutionIsUpdated() {
        if(pollutionLevel.isChanged(true)) {
            map.getNotifier().pollutionLevelChanged(this);
        }
    }

    //------------------------------------------------------------
    /**
     * Pollution Level を取得。
     */
    public PollutionLevelInfo getPollutionLevel() {
        return pollutionLevel ;
    }

    //------------------------------------------------------------
    /**
     * Pollution されているかどうか。
     */
    public boolean isPolluted() {
        return pollutionLevel.isPolluted() ;
    }

    //------------------------------------------------------------
    /**
     * 包含判定。
     */
    public abstract boolean contains(Point2D point);

    //------------------------------------------------------------
    /**
     * 包含判定。
     */
    public abstract boolean contains(Point3D point);

    //------------------------------------------------------------
    /**
     * 交差判定。
     */
    public abstract boolean intersectsLine(Line2D line);

    //------------------------------------------------------------
    /**
     * 形状を取得。
     */
    public abstract Shape getShape();

    //------------------------------------------------------------
    /**
     * 高さの範囲の最小値を取得。
     */
    public abstract double getMinHeight();

    //------------------------------------------------------------
    /**
     * 高さの範囲の最大値を取得。
     */
    public abstract double getMaxHeight();

    //------------------------------------------------------------
    /**
     * 頂点リスト。
     */
    public abstract ArrayList<Point2D> getAllVertices();	// tkokada

    //------------------------------------------------------------
    /**
     * 方向。
     */
    public abstract double getAngle();	// tkokada

    //------------------------------------------------------------
    /**
     * 描画。
     */
    public abstract void drawInEditor(Graphics2D g);

    //------------------------------------------------------------
    /**
     * For view customize : contact agent with pollution_area.
     */
    public static void showAttributeDialog(ArrayList<MapArea> areas,
                                           int x, int y) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog implements ActionListener, KeyListener {

            private ArrayList<MapArea> areas;

            private ArrayList<String> tags;

            private JTextField[] tagFields;
            boolean singleArea;

            public AttributeSetDialog(ArrayList<MapArea> _areas) {
                super();

                this.setModal(true);
                areas = _areas;

                setUpPanel();
            }

            private void setUpPanel() {
                Container contentPane = getContentPane();
                contentPane.add(OBNode.setupTagPanel(areas, this));

                this.pack();
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    for (MapArea area : areas) {
                        if (area.selected) {
                            if (singleArea) {
                                for (int i = 0; i < tags.size(); ++i) {
                                    String str = tagFields[i + 2].getText();
                                    area.tags.set(i, str);
                                }

                                String new_tag_str = tagFields[1].getText();
                                if (!new_tag_str.equals("")) {
                                    area.tags.add(new_tag_str);
                                }
                            } else {
                                String new_tag_str = tagFields[0].getText();
                                area.tags.add(new_tag_str);
                            }
                        }
                    }
                    this.dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    this.dispose();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {

            }
        }

        AttributeSetDialog dialog = new AttributeSetDialog(areas);
        dialog.setLocation(x, y);
        dialog.setVisible(true);
    }

    //------------------------------------------------------------
    /**
     * to/from DOM codes
     */
    static public String getNodeTypeString() {
        return "Area";
    }

    //------------------------------------------------------------
    /**
     * タイプ。
     */
    @Override
    public NType getNodeType() {
        return NType.AREA;
    }

    //------------------------------------------------------------
    /**
     * 末端判定。
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
