package sketchobj.stmts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constraintfactory.ConstData;
import constraintfactory.ConstraintFactory;
import constraintfactory.ExternalFunction;
import sketchobj.core.Context;
import sketchobj.core.SketchObject;
import sketchobj.core.Type;
import sketchobj.expr.ExprArrayRange;
import sketchobj.expr.ExprConstInt;
import sketchobj.expr.ExprConstant;
import sketchobj.expr.ExprFunCall;
import sketchobj.expr.ExprUnary;
import sketchobj.expr.ExprVar;
import sketchobj.expr.Expression;

public class StmtIfThen extends Statement {
	private Expression cond;
	private Statement cons, alt;
	private boolean isSingleFunCall = false;
	private boolean isSingleVarAssign = false;

	/**
	 * Create a new conditional statement, with the specified condition,
	 * consequent, and alternative. The two statements may be null if omitted.
	 * @param i 
	 */
	public StmtIfThen(Expression cond, Statement cons, Statement alt, int i) {
		this.cond = cond;
		cond.setParent(this);
		this.cons = cons;
		cons.setParent(this);
		this.alt = alt;
		if(alt!=null)
		alt.setParent(this);
		this.setLineNumber(i);
	}
	public StmtIfThen(Expression cond, Statement cons, Statement alt) {
		this(cond,cons,alt,0);
	}

	@Override
	public StmtIfThen clone() {
		if (alt != null)
			return new StmtIfThen(cond.clone(),cons.clone(), alt.clone(), this.getLineNumber());
		else 
			return new StmtIfThen(cond.clone(),cons.clone(), null, this.getLineNumber());
	}

	// @Override
	// public int size() {
	// int sz_cons = cons == null ? 0 : cons.size();
	// int sz_alt = alt == null ? 0 : alt.size();
	// return sz_cons + sz_alt;
	// }

	/** Returns the condition of this. */
	public Expression getCond() {
		return cond;
	}

	/**
	 * Returns the consequent statement of this, which is executed if the
	 * condition is true.
	 */
	public Statement getCons() {
		return cons;
	}

	/**
	 * Return the alternative statement of this, which is executed if the
	 * condition is false.
	 */
	public Statement getAlt() {
		return alt;
	}

	public boolean isSingleFunCall() {
		return isSingleFunCall;
	}

	public void singleFunCall() {
		isSingleFunCall = true;
	}

	public boolean isSingleVarAssign() {
		return isSingleVarAssign;
	}

	public void singleVarAssign() {
		isSingleVarAssign = true;
	}

	public String toString() {
		String result = "if(" + this.cond + "){\n";
		result += this.cons;
		result += "\n}";
		if (this.alt != null) {
			result += "else{\n";
			result += this.alt + "\n}";
		} else {
			result += "\n";
		}
		return result;
	}
	
	@Override
	public String toString_Context(){
		String result = "if(" + this.cond + "){\n";
		result += this.cons.toString_Context();
		result += "}";
		if (this.alt != null) {
			result += "else{\n";
			result += this.alt.toString_Context() + "}\n";
		} else {
			result += "\n";
		}
		return result + ": "+this.getPostctx().toString();
	}
	
	@Override
	public ConstData replaceConst(int index) {
		List<SketchObject> toAdd = new ArrayList<SketchObject>();
		toAdd.add(cons);
		if (alt != null)
			toAdd.add(alt);
		if (cond instanceof ExprConstant) {
			int value = ((ExprConstant) cond).getVal();
			Type t = ((ExprConstant) cond).getType();
			cond = new ExprFunCall("Const" + index, new ArrayList<Expression>());
			return new ConstData(t, toAdd, index + 1, value,null,this.getLineNumber());
		}
		return new ConstData(null, toAdd, index, 0,null,this.getLineNumber());
	}

	@Override
	public ConstData replaceConst_Exclude_This(int index, List<Integer> repair_range) {

		List<SketchObject> toAdd = new ArrayList<SketchObject>();
		toAdd.add(cons);
		if (alt != null)
			toAdd.add(alt);
		return new ConstData(null, toAdd, index, 0,null,this.getLineNumber());
	}
	
	@Override
	public Context buildContext(Context prectx, int sposition, List<Statement> originalStatements) {
		prectx = new Context(prectx);
		prectx.setLinenumber(this.getLineNumber());
		this.setPrectx(prectx);
		this.setPostctx(prectx);
		Context postctx = new Context(prectx);
		postctx.pushNewVars();;
		StmtBlock sb = new StmtBlock(ConstraintFactory.recordStateOriginal(this.getPrectx().getLinenumber(), 
				this.getPrectx().getAllVars()),this);
		for (Statement st : sb.stmts)
		{
		//	System.out.println("sb statement: "  + st);
		}
		List<Statement> s = new ArrayList<Statement>();
		postctx = cons.buildContext(postctx, sposition, s);
		StmtBlock consequent = new StmtBlock(cons,ConstraintFactory.recordStateOriginal(cons.getPostctx().getLinenumber(),
				cons.getPostctx().getAllVars()));
		for (Statement st : consequent.stmts)
		{
			//System.out.println("consequent statement: "  + st);
		}
		postctx.popVars();
		if (alt != null) {
			postctx.pushNewVars();;
			postctx = alt.buildContext(postctx, sposition, s);
			StmtBlock alternative = new StmtBlock(alt,ConstraintFactory.recordStateOriginal(alt.getPostctx().getLinenumber(),
					alt.getPostctx().getAllVars()));
			for (Statement st : consequent.stmts)
			{
				//System.out.println("consequent statement: "  + st);
			}
			postctx.popVars();
		}
		prectx.setVarsInScope(postctx.getVarsInScope());
		return prectx;
	}

	@Override
	public Map<String, Type> addRecordStmt(StmtBlock parent, int index, Map<String, Type> m) {
		List stmts = new ArrayList(parent.stmts);
		parent.stmts = stmts;
		parent.stmts.set(index,
				new StmtBlock(ConstraintFactory.recordState(this.getPrectx().getLinenumber(), 
						this.getPrectx().getAllVars()),this));
		
		this.cons = new StmtBlock(cons,ConstraintFactory.recordState(cons.getPostctx().getLinenumber(),
				cons.getPostctx().getAllVars()));
		m.putAll(((StmtBlock) cons).stmts.get(0).addRecordStmt((StmtBlock) cons, 0, m));
		if (alt != null) {
			this.alt = new StmtBlock(alt,ConstraintFactory.recordState(alt.getPostctx().getLinenumber(),
					alt.getPostctx().getAllVars()));
			m.putAll(((StmtBlock) alt).stmts.get(0).addRecordStmt((StmtBlock) alt, 0, m));
		}
		return m;
	}

	public void addRecordStmtOriginal(StmtBlock parent, int index) {
		List<Statement> stmts = new ArrayList(parent.stmts);
		parent.stmts = stmts;
		parent.stmts.set(index,
				new StmtBlock(ConstraintFactory.recordStateOriginal(this.getPrectx().getLinenumber(), 
						this.getPrectx().getAllVars()),this));
		
		this.cons = new StmtBlock(cons,ConstraintFactory.recordStateOriginal(cons.getPostctx().getLinenumber(),
				cons.getPostctx().getAllVars()));
		((StmtBlock) cons).stmts.get(0).addRecordStmtOriginal((StmtBlock) cons, 0);
		if (alt != null) {
			this.alt = new StmtBlock(alt,ConstraintFactory.recordStateOriginal(alt.getPostctx().getLinenumber(),
					alt.getPostctx().getAllVars()));
			((StmtBlock) alt).stmts.get(0).addRecordStmtOriginal((StmtBlock) alt, 0);
		}
	}

	@Override
	public boolean isBasic() {
		return true;
	}
	@Override
	public List<ExternalFunction> extractExternalFuncs(List<ExternalFunction> externalFuncNames) {
		externalFuncNames = cond.extractExternalFuncs(externalFuncNames);
		externalFuncNames = cons.extractExternalFuncs(externalFuncNames);
		if(alt!=null)
			externalFuncNames = alt.extractExternalFuncs(externalFuncNames);
		return externalFuncNames ;
	}
	@Override
	public ConstData replaceLinearCombination(int index) {
		List<SketchObject> toAdd = new ArrayList<SketchObject>();
		cond.setCtx(this.getPostctx());
		cond.setBoolean(true);
		toAdd.add(cond);
		
		toAdd.add(cons);
		if (alt != null){
			toAdd.add(alt);
		}
		return new ConstData(null, toAdd, index, 0,null,this.getLineNumber());
	}
	@Override
	public Map<Integer, String> ConstructLineToString(Map<Integer, String> line_to_string) {
		String result = "if(" + this.cond + ")";
		line_to_string.put(this.getLineNumber(), result);
		line_to_string = cons.ConstructLineToString(line_to_string);
		if(alt != null){
			line_to_string = alt.ConstructLineToString(line_to_string);
		}
		return line_to_string;
	}

}