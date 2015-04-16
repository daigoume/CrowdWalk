// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Engine
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/02/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/02/15]: Create This File. </LI>
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
import nodagumi.Itk.* ;

//======================================================================
/**
 * 思考エンジン.
 *
 * ルールは、以下で定義される形式
 * <pre>
 *   _rule_ ::= _expr_
 *   _expr_ ::= _null_ | [_expr_,_expr_,...] | _headedTerm_
 *   _null_ ::= null | {}
 *   _headedTerm_ ::= {"" : _head_, (_argKey_ : _expr_)*}
 *   _head_ ::= _String_
 *   _argKey_ ::= _String_
 * </pre>
 *
 * <h3> Special Forms </h3>
 * <ul>
 *  <li> _null_ は, NOP (No Operation)扱い。
 *  <li> [_expr_, ...] の形式は、{"" : "proc", "body" : [_expr_, ...]} と同じ。
 *  <li> _headedTerm_ で以下は組み込み。
 *    <pre>
 *     {"" : "if", "condition" : _expr_, "then" : _expr_ (, "else" : _expr_)?}
 *     {"" : "proc", "body" : [_expr_, _expr_, ...]}
 *     {"" : "not", "body" : _expr_}
 *     {"" : "and", "body" : [_expr_, _expr_, ...]}
 *     {"" : "or", "body" : [_expr_, _expr_, ...]}
 *     {"" : "true"}  ："true" と同じ
 *     {"" : "false"} ："false" と同じ
 *     {"" : "null"} : "null"  と同じ
 *     {"" : "quote", "value" : _expr_}
 *   </pre>
 * </ul>
 * <h3> 実処理用の _headedTerm_</h3>
 * <ul>
 *   <li>{@link #think_log "log"}</li>
 *   <li>{@link #think_getValue "getValue"}</li>
 *   <li>{@link #think_agentHasTag "agentHasTag"}</li>
 *   <li>{@link #think_placeHasTag "placeHasTag"}</li>
 *   <li>{@link #think_listenAlert "listenAlert"}</li>
 *   <li>{@link #think_clearAlert "clearAlert"}</li>
 *   <li>{@link #think_clearAllAlert "clearAllAlert"}</li>
 *   <li>{@link #think_changeGoal "changeGoal"}</li>
 *   <li>{@link #think_clearPlannedRoute "clearPlannedRoute"}</li>
 *   <li>{@link #think_insertRoute "insertRoute"}</li>
 * </ul>
 * また、上記にマッチしないアトム（配列でもオブジェクトでもないデータ）は、
 * その値を直接返す。（リテラル扱い）
 */
public class ThinkEngine {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント
     */
    private AgentBase agent = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ルールセット
     */
    private Term rule = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * log tag
     */
    static public String LogTagPrefix = "Think:" ;
    public String logTag() {
        if(agent == null) {
            return LogTagPrefix + "(null)" ;
        } else {
            return LogTagPrefix + agent.toString() ;
        }
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(){
        setAgent(null) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent){
        setAgent(_agent) ;
        setRule(null) ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public ThinkEngine(AgentBase _agent, Term _rule){
        setAgent(_agent) ;
        setRule(_rule) ;
    }

    //------------------------------------------------------------
    // アクセス
    //------------------------------------------------------------
    /**
     * set agent
     */
    public AgentBase setAgent(AgentBase _agent) {
        agent = _agent ;
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * get agent
     */
    public AgentBase getAgent() {
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * get agent
     */
    public HashMap<Term, Double> getAlertedMessageTable() {
        RationalAgent agent = (RationalAgent)getAgent() ;
        return agent.alertedMessageTable ;
    }

    //------------------------------------------------------------
    /**
     * set rule
     */
    public Term setRule(Term _rule) {
        rule = _rule ;
        return rule ;
    }

    //------------------------------------------------------------
    /**
     * get rule
     */
    public Term getRule() {
        return rule ;
    }

    //------------------------------------------------------------
    /**
     * 推論(top)
     */
    public Term think() {
        return think(rule) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(本体)
     */
    public Term think(Term expr) {
        if(expr == null || expr.isNull()) {
            return ThinkFormula.Term_Null ;
        } else if(expr.isArray()) {
            return think_procBody(expr) ;
        } else if(!(expr.getHead() instanceof String)) {
            // maybe, true or false or numbers
            return expr ;
        } else {
            String head = expr.getHeadString() ;
            if(head.equals("null")) {
                return ThinkFormula.Term_Null ;
            } else if(head.equals("true")) {
                return ThinkFormula.Term_True ;
            } else if(head.equals("false")) {
                return ThinkFormula.Term_False ;
            } else if(head.equals("not")) {
                return think_not(expr) ;
            } else if(head.equals("and")) {
                return think_and(expr) ;
            } else if(head.equals("or")) {
                return think_or(expr) ;
            } else if(head.equals("proc")) {
                return think_proc(expr) ;
            } else if(head.equals("if")) {
                return think_if(expr) ;
            } else if(head.equals("quote")) {
                return expr.getArgTerm("value") ;
            } else {
                return thinkGeneric(head, expr) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(not)。
     * <pre>
     * { "" : "not",
     *   "body" : _expr_ }
     * </pre>
     */
    public Term think_not(Term expr) {
        Term result = think(expr.getArgTerm("body")) ;
        if(checkFalse(result)) {
            return ThinkFormula.Term_True ;
        } else {
            return ThinkFormula.Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * False かどうかのチェック
     */
    public boolean checkFalse(Term expr) {
        return (expr == null ||
                expr.equals(ThinkFormula.Term_Null) ||
                expr.equals(ThinkFormula.Term_False)) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(And)。
     * <pre>
     * { "" : "and",
     *   "body" : [_expr_, _expr_, ...] }
     * </pre>
     */
    public Term think_and(Term expr) {
        Term body = expr.getArgTerm("body") ;
        if(!body.isArray()) {
            Itk.logError("Illegal and body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = ThinkFormula.Term_True ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = think(subExpr) ;
            if(checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(Or)。
     * <pre>
     * { "" : "or",
     *   "body" : [ _expr_, _expr_, ...] }
     * </pre>
     */
    public Term think_or(Term expr) {
        Term body = expr.getArgTerm("body") ;
        if(!body.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = ThinkFormula.Term_False ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = think(subExpr) ;
            if(!checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)。
     * <pre>
     * { "" : "proc",
     *   "body" : [ _expr_, _expr_, ...] }
     * OR
     * [ _expr, _expr_, ...]
     * </pre>
     */
    public Term think_proc(Term expr) {
        Term body = expr.getArgTerm("body") ;
        return think_procBody(body) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)の本体。
     */
    public Term think_procBody(Term body) {
        if(!body.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("body:", body) ;
            System.exit(1) ;
        }

        Term result = ThinkFormula.Term_Null ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = think(subExpr) ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(if)。
     * <pre>
     * { "" : "if",
     *   "condition" : _expr_,
     *   "then" : _expr_,
     *   "else" : _expr_ }
     * </pre>
     */
    public Term think_if(Term expr) {
        Term condition = expr.getArgTerm("condition") ;
        Term thenExpr = expr.getArgTerm("then") ;
        Term elseExpr = expr.getArgTerm("else") ;

        if(condition == null || thenExpr == null) {
            Itk.logError("wrong if expr", expr) ;
            System.exit(1) ;
        }

        Term cond = think(condition) ;

        if(!checkFalse(cond)) {
            return think(thenExpr) ;
        } else {
            if(elseExpr != null) {
                return think(elseExpr) ;
            } else {
                return ThinkFormula.Term_False ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(generic)
     */
    public Term thinkGeneric(String head, Term expr) {
        if(head.equals("log")) {
            return think_log(head, expr) ;
        } else if(head.equals("getValue")) {
            return think_getValue(head, expr) ;
        } else if(head.equals("agentHasTag")) {
            return think_agentHasTag(head, expr) ;
        } else if(head.equals("placeHasTag")) {
            return think_placeHasTag(head, expr) ;
        } else if(head.equals("listenAlert")) {
            return think_listenAlert(head, expr) ;
        } else if(head.equals("clearAlert")) {
            return think_clearAlert(head, expr) ;
        } else if(head.equals("clearAllAlert")) {
            return think_clearAllAlert(head, expr) ;
        } else if(head.equals("changeGoal")) {
            return think_changeGoal(head, expr) ;
        } else if(head.equals("clearPlannedRoute")) {
            return think_clearPlannedRoute(head, expr) ;
        } else if(head.equals("insertRoute")) {
            return think_insertRoute(head, expr) ;
        } else if(expr.isAtom()) { 
            // expr がアトムで、かつ予約語でなければ、そのまま返す。
            return expr ;
        } else {
            Itk.logWarn("unknown expression", "expr=", expr) ;
            return ThinkFormula.Term_Null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(log)。
     * <pre>
     *   {"":"log",
     *    "special": ("alertMessages" | "tags" | ...)}
     *  OR
     *   {"":"log",
     *    "tag": _StringForTag_,
     *    "value": _expr_}
     * </pre>
     * "special":"alertMessages" の場合は、
     * エージェントが取得している alert message のリストが表示される。
     * "special":"tags" の場合は、
     * エージェントが保持している tag のリストが表示される。
     */
    public Term think_log(String head, Term expr) {
        Term special = expr.getArgTerm("special") ;
        if(special != null) {
            if(special.equals("alertMessages")) {
                think_logAlertMessages() ;
            } else if(special.equals("tags")) {
                think_logTags() ;
            } else {
                Itk.logError("unknown log special:", expr) ;
            }
        } else {
            String tag = expr.getArgString("tag") ;
            Term value = expr.getArgTerm("value") ;
            Term result = ((value != null) ? think(value) : null) ;

            if(result == null) {
                Itk.logInfo(logTag(), tag) ;
            } else {
                Itk.logInfo(logTag(), tag, ":", result) ;
            }
        }
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(log alertMessages)。
     */
    public void think_logAlertMessages() {
        if(agent == null) {
            Itk.logInfo(logTag(), "no agent") ;
        } else {
            Itk.logInfo(logTag(), "alertMessages",
                        "time=", agent.currentTime) ;
            for(Map.Entry<Term, Double> entry :
                    getAlertedMessageTable().entrySet()) {
                Term message = entry.getKey() ;
                double time = entry.getValue() ;
                Itk.logInfo(LogTagPrefix, message, time) ;
            }
            Itk.logInfo(LogTagPrefix, "-----------------") ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(log tags)。
     */
    public void think_logTags() {
        if(agent == null) {
            Itk.logInfo(logTag(), "no agent") ;
        } else {
            Itk.logInfo(logTag(), "tags=", agent.getTags()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(getValue)。
     * <pre>
     * { "" : "getValue",
     *   "name" : _nameOfValue_,
     *  ("type" : _RandomType_,)  ;;; only for "random"
     *  ("max" : _integer_)     ;;; only for "random"/"int"
     * }
     *  _nameOfValue_ ::= "name" | "currentTime" | "agentId" | "random"
     *  _RandomType_ ::= "int" | "double"
     * </pre>
     */
    public Term think_getValue(String head, Term expr) {
        String name = expr.getArgString("name") ;
        if(name.equals("currentTime")) {
            return new Term(agent.currentTime) ;
        } else if(name.equals("agentId")) {
            return new Term(agent.ID) ;
        } else if(name.equals("random")) {
            String type = expr.getArgString("type") ;
            if(type == null || type.equals("int")) {
                if(expr.hasArg("max")) {
                    return new Term(agent.getRandomInt(expr.getArgInt("max"))) ;
                } else {
                    return new Term(agent.getRandomInt()) ;
                }
            } else if(type.equals("double")) {
                return new Term(agent.getRandomDouble()) ;
            } else {
                Itk.logError("unknown type for random in getValue.", 
                             "type=", type) ;
            return ThinkFormula.Term_Null ;
            }
        } else {
            Itk.logError("unknown parameter name for getValue.", "name=",name) ;
            return ThinkFormula.Term_Null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(agentHasTag)。
     * エージェント自身があるタグを持っているか？
     * <pre>
     * { "" : "agentHasTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term think_agentHasTag(String head, Term expr) {
        String tag = expr.getArgString("tag") ;
        if(agent.hasTag(tag)) {
            return ThinkFormula.Term_True ;
        } else {
            return ThinkFormula.Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(placeHasTag)。
     * 現在いるリンクがあるタグを持っているか？
     * <pre>
     * { "" : "placeHasTag",
     *   "tag" : _tag_ }
     * _tag ::= _String_
     * </pre>
     */
    public Term think_placeHasTag(String head, Term expr) {
        String tag = expr.getArgString("tag") ;
        if(agent.getCurrentLink().hasTag(tag)) {
            return ThinkFormula.Term_True ;
        } else {
            return ThinkFormula.Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(listenAlert)。
     * <pre>
     * { "" : "listenAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     * {@code "message"} は、
     * {@link nodagumi.ananPJ.Scenario.Scenario Scenario} の中の
     * {@link nodagumi.ananPJ.Scenario.AlertEvent AlertEvent} 参照。
     */
    public Term think_listenAlert(String head, Term expr) {
        Term message = expr.getArgTerm("message") ;
        Double time = getAlertedMessageTable().get(message) ;
        if(time != null) {
            return ThinkFormula.Term_True ;
        } else {
            return ThinkFormula.Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAlert)。
     * <pre>
     * { "" : "clearAlert",
     *   "message" : _alertMessage_ }
     * _alertMessage_ ::= _String_
     * </pre>
     */
    public Term think_clearAlert(String head, Term expr) {
        Term message = expr.getArgTerm("message") ;
        getAlertedMessageTable().remove(message) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clearAllAlert)。
     * <pre>
     * { "" : "clearAllAlert" }
     * </pre>
     */
    public Term think_clearAllAlert(String head, Term expr) {
        getAlertedMessageTable().clear() ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(changeGoal)。
     * <pre>
     * { "" : "changeGoal",
     *   "goal" : _goalTag_ }
     * _goalTag_ ::= _String_
     * </pre>
     */
    public Term think_changeGoal(String head, Term expr) {
        Term goalTag = expr.getArgTerm("goal") ;
        agent.changeGoal(goalTag) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(clear PlannedRoute)。
     * <pre>
     * { "" : "clearPlannedRoute" }
     * </pre>
     */
    public Term think_clearPlannedRoute(String head, Term expr) {
        agent.setPlannedRoute(new ArrayList<Term>(), true) ;
        return ThinkFormula.Term_True ;
    }

    //------------------------------------------------------------
    /**
     * 推論(insert route)。
     * <pre>
     * { "" : "insertRoute",
     *   "route" : _route_ }
     * _route_ ::= _tag_ || [_tag_, _tag_, ...]
     * </pre>
     */
    public Term think_insertRoute(String head, Term expr) {
        Term route = expr.getArgTerm("route") ;
        if(route.isArray()) {
            for(int i = route.getArray().size() ; i > 0 ; i--) {
                agent.insertRouteTagSafely(route.getNthTerm(i-1)) ;
            }
        } else {
            agent.insertRouteTagSafely(route) ;
        }
        return ThinkFormula.Term_True ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkEngine

