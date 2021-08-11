package de.uni_freiburg.informatik.ultimate.smtinterpol.proof;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashSet;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.LambdaTerm;
import de.uni_freiburg.informatik.ultimate.logic.PrintTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBConstants;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.convert.SMTAffineTerm;

public class ProofRules {
	public final static String RES = "res";
	public final static String ASSUME = "assume";
	public final static String ORACLE = "oracle";

	public final static String FALSEE = "false-";
	public final static String TRUEI = "true+";
	public final static String NOTI = "not+";
	public final static String NOTE = "not-";
	public final static String ORI = "or+";
	public final static String ORE = "or-";
	public final static String ANDI = "and+";
	public final static String ANDE = "and-";
	public final static String IMPI = "=>+";
	public final static String IMPE = "=>-";
	public final static String IFFI1 = "=+1";
	public final static String IFFI2 = "=+2";
	public final static String IFFE1 = "=-1";
	public final static String IFFE2 = "=-2";
	public final static String XORI = "xor+";
	public final static String XORE = "xor-";
	public final static String FORALLI = "forall+";
	public final static String FORALLE = "forall-";
	public final static String EXISTSI = "exists+";
	public final static String EXISTSE = "exists-";
	// equality chains of length >=3
	public final static String EQI = "=+";
	public final static String EQE = "=-";
	public final static String DISTINCTI = "distinct+";
	public final static String DISTINCTE = "distinct-";

	public final static String ITE1 = "ite1";
	public final static String ITE2 = "ite2";
	public final static String REFL = "refl";
	public final static String SYMM = "symm";
	public final static String TRANS = "trans";
	public final static String CONG = "cong";
	public final static String EXPAND = "expand";
	public final static String DELANNOT = "del!";

	// rules for linear arithmetic
	public final static String DIVISIBLE = "divisible-def";
	public final static String GTDEF = ">def";
	public final static String GEQDEF = ">=def";
	public final static String TRICHOTOMY = "trichotomy";
	public final static String EQLEQ = "eqleq";
	public final static String TOTAL = "total";
	public final static String TOTALINT = "total-int";
	public final static String FARKAS = "farkas";

	// axioms for arrays
	public final static String SELECTSTORE1 = "selectstore1";
	public final static String SELECTSTORE2 = "selectstore2";
	public final static String EXTDIFF = "extdiff";


	/**
	 * sort name for proofs.
	 */
	public final static String PROOF = "Proof";
	public final static String AXIOM = "axiom";
	public final static String CHOOSE = "choose";

	public final static String PREFIX = "..";

	public final static String ANNOT_VALUES = ":values";
	public final static String ANNOT_COEFFS = ":coeffs";
	public final static String ANNOT_DIVISOR = ":divisor";
	public final static String ANNOT_POS = ":pos";
	public final static String ANNOT_UNIT = ":unit";
	public final static String ANNOT_DEFINE_FUN = ":define-fun";

	public ProofRules(final Theory theory) {
		mTheory = theory;
		setupTheory();
		mAxiom = theory.term(PREFIX + AXIOM);
	}

	private final Theory mTheory;
	private final Term mAxiom;

	private void setupTheory() {

		if (mTheory.getDeclaredSorts().containsKey(PREFIX + PROOF)) {
			return;
		}

		mTheory.declareInternalSort(PREFIX + PROOF, 0, 0);
		final Sort proofSort = mTheory.getSort(PREFIX + PROOF);
		final Sort boolSort = mTheory.getBooleanSort();
		final Sort[] generic = mTheory.createSortVariables("X");
		final Sort[] bool1 = new Sort[] { boolSort };
		final Sort[] sort0 = new Sort[0];

		mTheory.declareInternalFunction(PREFIX + RES, new Sort[] { boolSort, proofSort, proofSort }, proofSort, 0);
		mTheory.declareInternalFunction(PREFIX + AXIOM, sort0, proofSort, 0);
		mTheory.declareInternalFunction(PREFIX + ASSUME, bool1, proofSort, 0);
		mTheory.declareInternalPolymorphicFunction(PREFIX + CHOOSE, generic, bool1, generic[0],
				FunctionSymbol.RETURNOVERLOAD);
	}

	public Term resolutionRule(final Term pivot, final Term proofPos, final Term proofNeg) {
		return mTheory.term(PREFIX + RES, pivot, proofPos, proofNeg);
	}

	public Term asserted(final Term t) {
		return mTheory.term(PREFIX + ASSUME, t);
	}

	public Term oracle(final ProofLiteral[] literals, final Annotation[] annots) {
		final Term[] atoms = new Term[literals.length];
		final BitSet bitset = new BitSet();
		for (int i = 0; i < literals.length; i++) {
			atoms[i] = literals[i].getAtom();
			bitset.set(i, literals[i].getPolarity());
		}
		return mTheory.annotatedTerm(annotate(":" + ORACLE, new Object[] { atoms, bitset }, annots), mAxiom);
	}

	public Term choose(final TermVariable tv, final Term formula) {
		final FunctionSymbol choose = mTheory.getFunctionWithResult(PREFIX + CHOOSE, null, tv.getSort(),
				formula.getSort());
		return mTheory.term(choose, mTheory.lambda(new TermVariable[] { tv }, formula));
	}

	public Term[] getSkolemVars(final TermVariable[] termVars, final Term subterm, final boolean isForall) {
		final Term[] skolemTerms = new Term[termVars.length];
		for (int i = 0; i < skolemTerms.length; i++) {
			Term subform = subterm;
			if (i + 1 < skolemTerms.length) {
				final TermVariable[] remainingVars = new TermVariable[skolemTerms.length - i - 1];
				System.arraycopy(termVars, i + 1, remainingVars, 0, remainingVars.length);
				subform = isForall ? mTheory.forall(remainingVars, subform) : mTheory.exists(remainingVars, subform);
			}
			if (isForall) {
				subform = mTheory.term(SMTLIBConstants.NOT, subform);
			}
			if (i > 0) {
				final TermVariable[] precedingVars = new TermVariable[i];
				final Term[] precedingSkolems = new Term[i];
				System.arraycopy(termVars, 0, precedingVars, 0, i);
				System.arraycopy(skolemTerms, 0, precedingSkolems, 0, i);
				subform = mTheory.let(precedingVars, precedingSkolems, subform);
			}
			skolemTerms[i] = choose(termVars[i], subform);
		}
		return skolemTerms;
	}

	private Annotation[] annotate(final String rule, final Object value, final Annotation... moreAnnots) {
		final Annotation[] annots = new Annotation[moreAnnots.length + 1];
		annots[0] = new Annotation(rule, value);
		System.arraycopy(moreAnnots, 0, annots, 1, moreAnnots.length);
		return annots;
	}

	public Term trueIntro() {
		return mTheory.annotatedTerm(annotate(":" + TRUEI, null), mAxiom);
	}

	public Term falseElim() {
		return mTheory.annotatedTerm(annotate(":" + FALSEE, null), mAxiom);
	}

	public Term notIntro(final Term notTerm) {
		assert ((ApplicationTerm) notTerm).getFunction().getName() == SMTLIBConstants.NOT;
		return mTheory.annotatedTerm(annotate(":" + NOTI, ((ApplicationTerm) notTerm).getParameters()), mAxiom);
	}

	public Term notElim(final Term notTerm) {
		assert ((ApplicationTerm) notTerm).getFunction().getName() == SMTLIBConstants.NOT;
		return mTheory.annotatedTerm(annotate(":" + NOTE, ((ApplicationTerm) notTerm).getParameters()), mAxiom);
	}

	public Term orIntro(final int pos, final Term orTerm) {
		assert ((ApplicationTerm) orTerm).getFunction().getName() == SMTLIBConstants.OR;
		return mTheory.annotatedTerm(
				annotate(":" + ORI, ((ApplicationTerm) orTerm).getParameters(), new Annotation(ANNOT_POS, pos)),
				mAxiom);
	}

	public Term orElim(final Term orTerm) {
		assert ((ApplicationTerm) orTerm).getFunction().getName() == SMTLIBConstants.OR;
		return mTheory.annotatedTerm(annotate(":" + ORE, ((ApplicationTerm) orTerm).getParameters()), mAxiom);
	}

	public Term andIntro(final Term andTerm) {
		assert ((ApplicationTerm) andTerm).getFunction().getName() == SMTLIBConstants.AND;
		return mTheory.annotatedTerm(annotate(":" + ANDI, ((ApplicationTerm) andTerm).getParameters()), mAxiom);
	}

	public Term andElim(final int pos, final Term andTerm) {
		assert ((ApplicationTerm) andTerm).getFunction().getName() == SMTLIBConstants.AND;
		return mTheory.annotatedTerm(
				annotate(":" + ANDE, ((ApplicationTerm) andTerm).getParameters(), new Annotation(ANNOT_POS, pos)),
				mAxiom);
	}

	public Term impIntro(final int pos, final Term impTerm) {
		assert ((ApplicationTerm) impTerm).getFunction().getName() == SMTLIBConstants.IMPLIES;
		return mTheory.annotatedTerm(
				annotate(":" + IMPI, ((ApplicationTerm) impTerm).getParameters(), new Annotation(ANNOT_POS, pos)),
				mAxiom);
	}

	public Term impElim(final Term impTerm) {
		assert ((ApplicationTerm) impTerm).getFunction().getName() == SMTLIBConstants.IMPLIES;
		return mTheory.annotatedTerm(annotate(":" + IMPE, ((ApplicationTerm) impTerm).getParameters()), mAxiom);
	}

	public Term iffIntro1(final Term iffTerm) {
		assert ((ApplicationTerm) iffTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		assert ((ApplicationTerm) iffTerm).getParameters().length == 2;
		assert ((ApplicationTerm) iffTerm).getParameters()[0].getSort().getName() == SMTLIBConstants.BOOL;
		return mTheory.annotatedTerm(annotate(":" + IFFI1, ((ApplicationTerm) iffTerm).getParameters()), mAxiom);
	}

	public Term iffIntro2(final Term iffTerm) {
		assert ((ApplicationTerm) iffTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		assert ((ApplicationTerm) iffTerm).getParameters().length == 2;
		assert ((ApplicationTerm) iffTerm).getParameters()[0].getSort().getName() == SMTLIBConstants.BOOL;
		return mTheory.annotatedTerm(annotate(":" + IFFI2, ((ApplicationTerm) iffTerm).getParameters()), mAxiom);
	}

	public Term iffElim1(final Term iffTerm) {
		assert ((ApplicationTerm) iffTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		assert ((ApplicationTerm) iffTerm).getParameters().length == 2;
		assert ((ApplicationTerm) iffTerm).getParameters()[0].getSort().getName() == SMTLIBConstants.BOOL;
		return mTheory.annotatedTerm(annotate(":" + IFFE1, ((ApplicationTerm) iffTerm).getParameters()), mAxiom);
	}

	public Term iffElim2(final Term iffTerm) {
		assert ((ApplicationTerm) iffTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		assert ((ApplicationTerm) iffTerm).getParameters().length == 2;
		assert ((ApplicationTerm) iffTerm).getParameters()[0].getSort().getName() == SMTLIBConstants.BOOL;
		return mTheory.annotatedTerm(annotate(":" + IFFE2, ((ApplicationTerm) iffTerm).getParameters()), mAxiom);
	}

	private Term xorAxiom(final String name, final Term[]... xorArgs) {
		assert checkXorParams(xorArgs);
		return mTheory.annotatedTerm(new Annotation[] { new Annotation(name, xorArgs) }, mAxiom);
	}

	public Term xorIntro(final Term[] xorArgs1, final Term[] xorArgs2, final Term[] xorArgs3) {
		return xorAxiom(":" + XORI, xorArgs1, xorArgs2, xorArgs3);
	}

	public Term xorElim(final Term[] xorArgs1, final Term[] xorArgs2, final Term[] xorArgs3) {
		return xorAxiom(":" + XORE, xorArgs1, xorArgs2, xorArgs3);
	}

	public Term forallIntro(final QuantifiedFormula forallTerm) {
		assert forallTerm.getQuantifier() == QuantifiedFormula.FORALL;
		return mTheory.annotatedTerm(annotate(":" + FORALLI,
				new Term[] { mTheory.lambda(forallTerm.getVariables(), forallTerm.getSubformula()) }), mAxiom);
	}

	public Term forallElim(final Term[] subst, final QuantifiedFormula forallTerm) {
		assert forallTerm.getQuantifier() == QuantifiedFormula.FORALL;
		return mTheory.annotatedTerm(
				annotate(":" + FORALLE,
						new Term[] { mTheory.lambda(forallTerm.getVariables(), forallTerm.getSubformula()) },
						new Annotation(ANNOT_VALUES, subst)),
				mAxiom);
	}

	public Term existsIntro(final Term[] subst, final QuantifiedFormula existsTerm) {
		assert existsTerm.getQuantifier() == QuantifiedFormula.EXISTS;
		return mTheory.annotatedTerm(
				annotate(":" + EXISTSI,
						new Term[] { mTheory.lambda(existsTerm.getVariables(), existsTerm.getSubformula()) },
						new Annotation(ANNOT_VALUES, subst)),
				mAxiom);
	}

	public Term existsElim(final QuantifiedFormula existsTerm) {
		assert existsTerm.getQuantifier() == QuantifiedFormula.EXISTS;
		return mTheory.annotatedTerm(
				annotate(":" + EXISTSE,
						new Term[] { mTheory.lambda(existsTerm.getVariables(), existsTerm.getSubformula()) }),
				mAxiom);
	}

	public Term equalsIntro(final Term eqTerm) {
		assert ((ApplicationTerm) eqTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		return mTheory.annotatedTerm(annotate(":" + EQI, ((ApplicationTerm) eqTerm).getParameters()), mAxiom);
	}

	public Term equalsElim(final int pos1, final int pos2, final Term eqTerm) {
		assert ((ApplicationTerm) eqTerm).getFunction().getName() == SMTLIBConstants.EQUALS;
		assert 0 <= pos1 && pos1 < ((ApplicationTerm) eqTerm).getParameters().length;
		assert 0 <= pos2 && pos2 < ((ApplicationTerm) eqTerm).getParameters().length;
		return mTheory.annotatedTerm(annotate(":" + EQE, ((ApplicationTerm) eqTerm).getParameters(),
				new Annotation(ANNOT_POS, new Integer[] { pos1, pos2 })), mAxiom);
	}

	public Term distinctIntro(final Term disTerm) {
		assert ((ApplicationTerm) disTerm).getFunction().getName() == SMTLIBConstants.DISTINCT;
		return mTheory.annotatedTerm(annotate(":" + DISTINCTI, ((ApplicationTerm) disTerm).getParameters()), mAxiom);
	}

	public Term distinctElim(final int pos1, final int pos2, final Term disTerm) {
		assert ((ApplicationTerm) disTerm).getFunction().getName() == SMTLIBConstants.DISTINCT;
		assert 0 <= pos1 && pos1 < ((ApplicationTerm) disTerm).getParameters().length;
		assert 0 <= pos2 && pos2 < ((ApplicationTerm) disTerm).getParameters().length;
		return mTheory.annotatedTerm(annotate(":" + DISTINCTE, ((ApplicationTerm) disTerm).getParameters(),
				new Annotation(ANNOT_POS, new Integer[] { pos1, pos2 })), mAxiom);
	}

	public Term refl(final Term term) {
		return mTheory.annotatedTerm(annotate(":" + REFL, new Term[] { term }), mAxiom);
	}

	public Term symm(final Term term1, final Term term2) {
		return mTheory.annotatedTerm(annotate(":" + SYMM, new Term[] { term1, term2 }), mAxiom);
	}

	public Term trans(final Term... terms) {
		assert terms.length > 2;
		return mTheory.annotatedTerm(annotate(":" + TRANS, terms), mAxiom);
	}

	public Term cong(final Term term1, final Term term2) {
		assert ((ApplicationTerm) term1).getFunction() == ((ApplicationTerm) term2).getFunction();
		assert ((ApplicationTerm) term1).getParameters().length == ((ApplicationTerm) term2).getParameters().length;
		final Annotation[] annot = new Annotation[] {
				new Annotation(":"+CONG, new Object[] {
						((ApplicationTerm) term1).getFunction(),
						((ApplicationTerm) term1).getParameters(),
						((ApplicationTerm) term2).getParameters(),
				})
		};
		return mTheory.annotatedTerm(annot, mAxiom);
	}

	public Term expand(final Term term) {
		final Annotation[] annot = new Annotation[] { new Annotation(":" + EXPAND,
				new Object[] { ((ApplicationTerm) term).getFunction(), ((ApplicationTerm) term).getParameters(), }) };
		return mTheory.annotatedTerm(annot, mAxiom);
	}

	public Term ite1(final Term iteTerm) {
		assert ((ApplicationTerm) iteTerm).getFunction().getName() == SMTLIBConstants.ITE;
		return mTheory.annotatedTerm(annotate(":" + ITE1, ((ApplicationTerm) iteTerm).getParameters()), mAxiom);
	}

	public Term ite2(final Term iteTerm) {
		assert ((ApplicationTerm) iteTerm).getFunction().getName() == SMTLIBConstants.ITE;
		return mTheory.annotatedTerm(annotate(":" + ITE2, ((ApplicationTerm) iteTerm).getParameters()), mAxiom);
	}

	public Term delAnnot(final Term annotTerm) {
		final Term subterm = ((AnnotatedTerm) annotTerm).getSubterm();
		final Annotation[] subAnnots = ((AnnotatedTerm) annotTerm).getAnnotations();
		return mTheory.annotatedTerm(annotate(":" + DELANNOT, new Object[] { subterm, subAnnots }), mAxiom);
	}

	public Term divisible(final Term lhs, final BigInteger divisor) {
		return mTheory.annotatedTerm(
				annotate(":" + DIVISIBLE, new Term[] { lhs }, new Annotation(ANNOT_DIVISOR, divisor)),
				mAxiom);
	}

	public Term gtDef(final Term greaterTerm) {
		assert ((ApplicationTerm) greaterTerm).getFunction().getName() == SMTLIBConstants.GT;
		return mTheory.annotatedTerm(annotate(":" + GTDEF, ((ApplicationTerm) greaterTerm).getParameters()), mAxiom);
	}

	public Term geqDef(final Term greaterTerm) {
		assert ((ApplicationTerm) greaterTerm).getFunction().getName() == SMTLIBConstants.GEQ;
		return mTheory.annotatedTerm(annotate(":" + GEQDEF, ((ApplicationTerm) greaterTerm).getParameters()), mAxiom);
	}

	public Term trichotomy(final Term lhs, final Term rhs) {
		return mTheory.annotatedTerm(annotate(":" + TRICHOTOMY, new Term[] { lhs, rhs }), mAxiom);
	}

	public Term eqLeq(final Term lhs, final Term rhs) {
		return mTheory.annotatedTerm(annotate(":" + EQLEQ, new Term[] { lhs, rhs }), mAxiom);
	}

	public Term total(final Term lhs, final Term rhs) {
		return mTheory.annotatedTerm(annotate(":" + TOTAL, new Term[] { lhs, rhs }), mAxiom);
	}

	/**
	 * Axiom for integer reasoning. This proves
	 *
	 * <pre>
	 * (+ (&lt;= x c) + (&lt;= (c+1) x))
	 * </pre>
	 *
	 * where x is a term of sort Int and c an integer constant. Here c+1 is the
	 * constant c increased by one.
	 *
	 * @param x a term of sort Int.
	 * @param c an integer constant.
	 * @return the axiom.
	 */
	public Term totalInt(final Term x, final BigInteger c) {
		return mTheory.annotatedTerm(annotate(":" + TOTALINT, new Object[] { x, c }), mAxiom);
	}

	public Term farkas(final Term[] inequalities, final BigInteger[] coefficients) {
		assert checkFarkas(inequalities, coefficients);
		return mTheory.annotatedTerm(annotate(":" + FARKAS, inequalities, new Annotation(ANNOT_COEFFS, coefficients)),
				mAxiom);
	}

	public Term selectStore1(final Term array, final Term index, final Term value) {
		return mTheory.annotatedTerm(annotate(":" + SELECTSTORE1, new Term[] { array, index, value }), mAxiom);
	}

	public Term selectStore2(final Term array, final Term index, final Term value, final Term index2) {
		return mTheory.annotatedTerm(annotate(":" + SELECTSTORE2, new Term[] { array, index, value, index2 }), mAxiom);
	}

	public Term extDiff(final Term array1, final Term array2) {
		return mTheory.annotatedTerm(annotate(":" + EXTDIFF, new Term[] { array1, array2 }), mAxiom);
	}

	public Term defineFun(final FunctionSymbol func, final Term definition, final Term subProof) {
		assert func.getName().startsWith("@");
		return mTheory.annotatedTerm(new Annotation[] {
				new Annotation(ANNOT_DEFINE_FUN, new Object[] { func, definition }),
		}, subProof);
	}

	public static void printProof(final Appendable appender, final Term proof) {
		new PrintProof().append(appender, proof);
	}

	public static boolean checkFarkas(final Term[] inequalities, final BigInteger[] coefficients) {
		if (inequalities.length != coefficients.length) {
			return false;
		}
		final SMTAffineTerm sum = new SMTAffineTerm();
		boolean strict = false;
		for (int i = 0; i < inequalities.length; i++) {
			if (coefficients[i].signum() != 1) {
				return false;
			}
			final ApplicationTerm appTerm = (ApplicationTerm) inequalities[i];
			final Term[] params = appTerm.getParameters();
			if (params.length != 2
					|| (appTerm.getFunction().getName() != SMTLIBConstants.LT
					&& appTerm.getFunction().getName() != SMTLIBConstants.LEQ)) {
				return false;
			}
			if (appTerm.getFunction().getName() == SMTLIBConstants.LT) {
				strict = true;
			}
			final SMTAffineTerm ineqAffine = new SMTAffineTerm(params[0]);
			ineqAffine.add(Rational.MONE, params[1]);
			ineqAffine.mul(Rational.valueOf(coefficients[i], BigInteger.ONE));
			sum.add(ineqAffine);
		}
		if (!sum.isConstant()) {
			return false;
		}
		return sum.getConstant().signum() >= (strict ? 0 : 1);
	}

	public static boolean checkXorParams(final Term[][] xorArgs) {
		assert xorArgs.length == 3;
		final HashSet<Term> xorSum = new HashSet<>();
		for (final Term[] args : xorArgs) {
			for (final Term arg : args) {
				if (xorSum.contains(arg)) {
					xorSum.remove(arg);
				} else {
					xorSum.add(arg);
				}
			}
		}
		return xorSum.isEmpty();
	}

	public boolean isAxiom(final Term proof) {
		return proof instanceof AnnotatedTerm && ((AnnotatedTerm) proof).getSubterm() == mAxiom;
	}

	public boolean isProofRule(final String rule, final Term proof) {
		return proof instanceof ApplicationTerm
				&& ((ApplicationTerm) proof).getFunction().getName().equals(PREFIX + rule);
	}

	public boolean isDefineFun(final Term proof) {
		return proof instanceof AnnotatedTerm
				&& ((AnnotatedTerm) proof).getAnnotations()[0].getKey() == ANNOT_DEFINE_FUN;
	}

	public static class PrintProof extends PrintTerm {
		@Override
		public void walkTerm(final Term proof) {
			if (proof instanceof AnnotatedTerm) {
				final AnnotatedTerm annotTerm = (AnnotatedTerm) proof;
				final Annotation[] annots = annotTerm.getAnnotations();
				if (annots.length == 1 && annots[0].getKey() == ANNOT_DEFINE_FUN) {
					final Object[] annotVal = (Object[]) annots[0].getValue();
					assert annotVal.length == 2;
					final FunctionSymbol func = (FunctionSymbol) annotVal[0];
					final LambdaTerm definition = (LambdaTerm) annotVal[1];
					mTodo.add(")");
					mTodo.add(annotTerm.getSubterm());
					mTodo.add(" ");
					mTodo.add(")");
					mTodo.add(definition.getSubterm());
					mTodo.add(" ");
					final TermVariable[] vars = definition.getVariables();
					for (int i = vars.length - 1; i >= 0; i--) {
						mTodo.add(vars[i].getSort());
						mTodo.add(" ");
						mTodo.add(vars[i]);
						mTodo.add(" ");
					}
					mTodo.add("((" + annots[0].getKey().substring(1));
					return;
				} else if (annotTerm.getSubterm() instanceof ApplicationTerm
						&& ((ApplicationTerm) annotTerm.getSubterm()).getFunction().getName() == PREFIX + AXIOM) {
					switch (annots[0].getKey()) {
					case ":" + ORACLE: {
						assert annots.length >= 1;
						final Object[] values = (Object[]) annots[0].getValue();
						assert values.length == 2;
						final Term[] atoms = (Term[]) values[0];
						final BitSet polarities = (BitSet) values[1];
						mTodo.add(")");
						for (int i = annots.length - 1; i >= 1; i--) {
							if (annots[i].getValue() != null) {
								mTodo.add(annots[i].getValue());
								mTodo.add(" ");
							}
							mTodo.add(annots[i].getKey());
							mTodo.add(" ");
						}
						mTodo.add(")");
						for (int i = atoms.length - 1; i >= 0; i--) {
							mTodo.add(atoms[i]);
							mTodo.add(polarities.get(i) ? " + " : " - ");
						}
						mTodo.add("(" + annots[0].getKey().substring(1) + " (");
						return;
					}
					case ":" + TRUEI:
					case ":" + FALSEE: {
						assert annots.length == 1;
						assert annots[0].getValue() == null;
						mTodo.add(annots[0].getKey().substring(1));
						return;
					}
					case ":" + NOTI:
					case ":" + NOTE:
					case ":" + ORE:
					case ":" + ANDI:
					case ":" + IMPE:
					case ":" + IFFI1:
					case ":" + IFFI2:
					case ":" + IFFE1:
					case ":" + IFFE2:
					case ":" + ITE1:
					case ":" + ITE2:
					case ":" + REFL:
					case ":" + SYMM:
					case ":" + TRANS:
					case ":" + EQI:
					case ":" + DISTINCTI:
					case ":" + GTDEF:
					case ":" + GEQDEF:
					case ":" + TRICHOTOMY:
					case ":" + EQLEQ:
					case ":" + TOTAL:
					case ":" + SELECTSTORE1:
					case ":" + SELECTSTORE2:
					case ":" + EXTDIFF: {
						final Term[] params = (Term[]) annots[0].getValue();
						assert annots.length == 1;
						mTodo.add(")");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
						}
						mTodo.add("(" + annots[0].getKey().substring(1));
						return;
					}
					case ":" + ORI:
					case ":" + ANDE:
					case ":" + IMPI: {
						final Term[] params = (Term[]) annots[0].getValue();
						assert annots.length == 2;
						assert annots[1].getKey() == ANNOT_POS;
						mTodo.add(")");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
						}
						mTodo.add(annots[1].getValue());
						mTodo.add("(" + annots[0].getKey().substring(1) + " ");
						return;
					}
					case ":" + EQE:
					case ":" + DISTINCTE: {
						final Term[] params = (Term[]) annots[0].getValue();
						assert annots.length == 2;
						assert annots[1].getKey() == ANNOT_POS;
						final Integer[] positions = (Integer[]) annots[1].getValue();
						mTodo.add(")");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
						}
						mTodo.add(positions[0] + " " + positions[1]);
						mTodo.add("(" + annots[0].getKey().substring(1) + " ");
						return;
					}
					case ":" + CONG: {
						assert annots.length == 1;
						final Object[] congArgs = (Object[]) annots[0].getValue();
						assert congArgs.length == 3;
						final FunctionSymbol func = (FunctionSymbol) congArgs[0];
						final Term[] params1 = (Term[]) congArgs[1];
						final Term[] params2 = (Term[]) congArgs[2];
						mTodo.add("))");
						for (int i = params2.length - 1; i >= 0; i--) {
							mTodo.add(params2[i]);
							mTodo.add(" ");
						}
						mTodo.add(func.getApplicationString());
						mTodo.add(") (");
						for (int i = params1.length - 1; i >= 0; i--) {
							mTodo.add(params1[i]);
							mTodo.add(" ");
						}
						mTodo.add(func.getApplicationString());
						mTodo.add("(" + annots[0].getKey().substring(1) + " (");
						return;
					}
					case ":" + XORI:
					case ":" + XORE: {
						assert annots.length == 1;
						final Term[][] xorLists = (Term[][]) annots[0].getValue();
						assert xorLists.length == 3;
						mTodo.add(")");
						for (int i = 2; i >= 0; i--) {
							mTodo.add(")");
							for (int j = xorLists[i].length - 1; j >= 0; j--) {
								mTodo.add(xorLists[i][j]);
								if (j > 0) {
									mTodo.add(" ");
								}
							}
							mTodo.add(" (");
						}
						mTodo.add("(" + annots[0].getKey().substring(1));
						return;
					}
					case ":" + EXPAND: {
						assert annots.length == 1;
						final Object[] expandParams = (Object[]) annots[0].getValue();
						assert expandParams.length == 2;
						final FunctionSymbol func = (FunctionSymbol) expandParams[0];
						final Term[] params = (Term[]) expandParams[1];
						mTodo.add(")");
						mTodo.add(") ");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
						}
						mTodo.add(func.getApplicationString());
						mTodo.add("(" + annots[0].getKey().substring(1) + " (");
						return;
					}
					case ":" + FORALLE:
					case ":" + EXISTSI: {
						assert annots.length == 2;
						final Term[] params = (Term[]) annots[0].getValue();
						final LambdaTerm lambda = (LambdaTerm) params[0];
						final TermVariable[] termVars = lambda.getVariables();
						assert annots[1].getKey() == ANNOT_VALUES;
						final Term[] values = (Term[]) annots[1].getValue();
						mTodo.add(")");
						mTodo.add(lambda.getSubterm());
						mTodo.add(") ");
						for (int i = termVars.length - 1; i >= 0; i--) {
							mTodo.add(")");
							mTodo.add(values[i]);
							mTodo.add(" ");
							mTodo.add(termVars[i]);
							mTodo.add(i == 0 ? "(" : " (");
						}
						mTodo.add("(" + annots[0].getKey().substring(1) + " (");
						return;
					}
					case ":" + FORALLI:
					case ":" + EXISTSE: {
						assert annots.length == 1;
						final Term[] params = (Term[]) annots[0].getValue();
						final LambdaTerm lambda = (LambdaTerm) params[0];
						final TermVariable[] termVars = lambda.getVariables();

						mTodo.add(")");
						mTodo.add(lambda.getSubterm());
						mTodo.add(") ");
						for (int i = termVars.length - 1; i >= 0; i--) {
							mTodo.add(")");
							mTodo.add(termVars[i].getSort());
							mTodo.add(" ");
							mTodo.add(termVars[i]);
							mTodo.add(i == 0 ? "(" : " (");
						}
						mTodo.add("(" + annots[0].getKey().substring(1) + " (");
						return;
					}
					case ":" + DELANNOT: {
						mTodo.add("))");
						assert annots.length == 1;
						final Object[] params = (Object[]) annots[0].getValue();
						assert params.length == 2;
						final Term subterm = (Term) params[0];
						final Annotation[] subAnnots = (Annotation[]) params[1];
						for (int i = subAnnots.length - 1; i >= 0; i--) {
							if (subAnnots[i].getValue() != null) {
								mTodo.addLast(subAnnots[i].getValue());
								mTodo.addLast(" ");
							}
							mTodo.addLast(" " + subAnnots[i].getKey());
						}
						mTodo.addLast(subterm);
						mTodo.add("(" + DELANNOT + " (! ");
						return;
					}
					case ":" + DIVISIBLE: {
						final Term[] params = (Term[]) annots[0].getValue();
						assert annots.length == 2;
						assert annots[1].getKey() == ANNOT_DIVISOR;
						mTodo.add(")");
						mTodo.add(annots[1].getValue());
						mTodo.add(" ");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
						}
						mTodo.add("(" + annots[0].getKey().substring(1));
						return;
					}
					case ":" + TOTALINT: {
						final Object[] params = (Object[]) annots[0].getValue();
						final BigInteger c = (BigInteger) params[1];
						final Term x = (Term) params[0];
						assert annots.length == 1;
						mTodo.add(")");
						if (c.signum() < 0) {
							mTodo.add("(- " + c.toString() + ")");
						} else {
							mTodo.add(c);
						}
						mTodo.add(" ");
						mTodo.add(x);
						mTodo.add("(" + annots[0].getKey().substring(1) + " ");
						return;
					}
					case ":" + FARKAS: {
						final Term[] params = (Term[]) annots[0].getValue();
						assert annots.length == 2;
						assert annots[1].getKey() == ANNOT_COEFFS;
						final BigInteger[] coeffs = (BigInteger[]) annots[1].getValue();
						assert params.length == coeffs.length;
						mTodo.add(")");
						for (int i = params.length - 1; i >= 0; i--) {
							mTodo.add(params[i]);
							mTodo.add(" ");
							mTodo.add(coeffs[i]);
							mTodo.add(" ");
						}
						mTodo.add("(" + annots[0].getKey().substring(1));
						return;
					}
					}
				}
			}

			if (proof instanceof ApplicationTerm) {
				final ApplicationTerm appTerm = (ApplicationTerm) proof;
				final Term[] params = appTerm.getParameters();
				switch (appTerm.getFunction().getName()) {
				case PREFIX + RES: {
					assert params.length == 3;
					mTodo.add(")");
					for (int i = params.length - 1; i >= 0; i--) {
						mTodo.add(params[i]);
						mTodo.add(" ");
					}
					mTodo.add("(" + RES);
					return;
				}
				case PREFIX + CHOOSE: {
					assert params.length == 1;
					final LambdaTerm lambda = (LambdaTerm) params[0];
					assert lambda.getVariables().length == 1;
					mTodo.add(")");
					mTodo.add(lambda.getSubterm());
					mTodo.add(") ");
					mTodo.add(lambda.getVariables()[0].getSort());
					mTodo.add(" ");
					mTodo.add(lambda.getVariables()[0]);
					mTodo.add("(" + CHOOSE + " (");
					return;
				}
				case PREFIX + ASSUME: {
					assert params.length == 1;
					mTodo.add(")");
					mTodo.add(params[0]);
					mTodo.add("(" + ASSUME + " ");
					return;
				}
				default:
					break;
				}
			}
			super.walkTerm(proof);
		}
	}
}
