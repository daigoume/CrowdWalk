// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk jRuby Utility
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/06/20 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/06/20]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.Itk;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;

import nodagumi.Itk.Itk;

//======================================================================
/**
 * jruby を呼び出すための簡易ルーチン
 */
public class ItkRuby {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby の実行系。
     * 基本、1インスタンスあればいいようなので、１つだけにしておく。
     */
    private static Ruby rubyEngine = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Ruby Scrupt の評価実行系。
     */
    private ScriptingContainer container ;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public ItkRuby() {
	ensureRubyEngine() ;
	container = new ScriptingContainer() ;
    }

    //------------------------------------------------------------
    /**
     * script 実行
     */
    public Object eval(String script) {
	return container.runScriptlet(script) ;
    }

    //------------------------------------------------------------
    /**
     * 変数設定(トップレベル)
     */
    public Object setVariable(String varName, Object value) {
	return container.put(varName, value) ;
    }

    //------------------------------------------------------------
    /**
     * 変数設定(オブジェクト内)
     */
    public Object setVariable(Object object, String varName, Object value) {
	return container.put(object, varName, value) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(トップレベル)
     */
    public Object getVariable(String varName){
	return container.get(varName) ;
    }

    //------------------------------------------------------------
    /**
     * 変数値取得(オブジェクト内)
     */
    public Object getVariable(Object object, String varName) {
	return container.get(object, varName) ;
    }

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(トップレベル)
     */
    /*
    public Object callMethod(String methodName, Object... args) {
	return container.callMethod(methodName, args) ;
    }
    */

    //------------------------------------------------------------
    /**
     * メソッド呼び出し(オブジェクト内)
     */
    public Object callMethod(Object object, String methodName,
			     Object... args) {
	return container.callMethod(object, methodName, args) ;
    }

    //------------------------------------------------------------
    /**
     * script のパース。
     * 返された EvalUnit に対して、run() メソッドを呼び出すと、実行される。
     * @return EvalUnit。
     */
    public EvalUnit parseScript(String script) {
	return container.parse(script) ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath。
     * @return path のリスト。
     */
    public List<String> getLoadPaths() {
	return container.getLoadPaths() ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath を設定。
     * @param pathList path のリスト。
     */
    public void setLoadPaths(List<String> pathList) {
	container.setLoadPaths(pathList) ;
    }

    //------------------------------------------------------------
    /**
     * Ruby の LoadPath の末尾に追加。
     * 追加するかどうかチェックする。
     * @param path 追加するpath。
     * @return 追加されれば true。すでに存在して追加されなかった場合は false。
     */
    public boolean pushLoadPath(String path) {
	List<String> pathList = this.getLoadPaths() ;
	if(pathList.contains(path)) {
	    return false ;
	} else {
	    pathList.add(path) ;
	    this.setLoadPaths(pathList) ;
	    return true ;
	}
    }

    //------------------------------------------------------------
    /**
     * Ruby の current directory を取得。
     * @return current directory。
     */
    public String getCurrentDirectory() {
	return container.getCurrentDirectory() ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine がインスタンスを持つか確認。
     * @return 新たにインスタンス生成されたら true。それ以外は false。
     */
    public static boolean ensureRubyEngine() {
	if(rubyEngine == null) {
	    rubyEngine = Ruby.newInstance() ;
	    return true ;
	} else {
	    return false ;
	}
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * rubyEngine 上で直接実行。
     */
    public static Object evalOnEngine(String script) {
	ensureRubyEngine() ;
	return rubyEngine.evalScriptlet(script) ;
    }

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------

} // class Foo

