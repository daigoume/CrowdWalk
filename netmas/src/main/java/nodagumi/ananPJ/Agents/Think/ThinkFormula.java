// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/15]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Think;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RationalAgent;
import nodagumi.ananPJ.Agents.Think.*;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考を表す式の処理系。
 * 組み込まれている Formula リスト
 * <ul>
 *  <li>{@link ThinkFormulaLogical#call Logical: 論理形式および実行制御}</li>
 *  <li>{@link ThinkFormulaMisc#call Misc: その他}</li>
 * </ul>
 */
abstract public class ThinkFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Formula Lexicon
     */
    static public Lexicon lexicon = new Lexicon() ;
    static {
        ThinkFormulaMisc.registerFormulas() ;
        ThinkFormulaLogical.registerFormulas() ;
        ThinkFormulaArithmetic.registerFormulas() ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を登録
     */
    static public void register(String name, ThinkFormula formula) {
	lexicon.register(name, formula) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    static public ThinkFormula findFormula(String head) {
	return (ThinkFormula)(lexicon.lookUp(head)) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * Formula を検索
     */
    static public ThinkFormula findFormula(Term head) {
	return (ThinkFormula)(lexicon.lookUp(head.getString())) ;
    }

    //============================================================
    // 特殊 Term
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * null
     */
    static public Term Term_Null = new Term() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * true
     */
    static public Term Term_True = new Term("true") ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * false
     */
    static public Term Term_False = new Term("false") ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkFormula(){}

    //------------------------------------------------------------
    /**
     * 呼び出し
     */
    abstract public Term call(String head, Term expr, ThinkEngine engine) ;

    //------------------------------------------------------------
    /**
     * False かどうかのチェック
     */
    public boolean checkFalse(Term expr) {
        return (expr == null ||
                expr.equals(Term_Null) ||
                expr.equals(Term_False)) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormula

