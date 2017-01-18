package de.uni_freiburg.informatik.ultimate.smtinterpol.theory.epr.dawgs;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface IDawg<LETTER, COLNAMES> extends Iterable<List<LETTER>> {
	
	public SortedSet<COLNAMES> getColnames();
	
	public int getArity();
	
//	public IDawg<LETTER, COLNAMES> join(IDawg<LETTER, COLNAMES> other);

	public IDawg<LETTER, COLNAMES> complement();
	
//	public IDawg<LETTER, COLNAMES> union(IDawg<LETTER, COLNAMES> other);
	
	public boolean accepts(List<LETTER> word);

	/**
	 * Add one point to this Dawg
	 * Requires:
	 *  - arguments.length equals the arity of this dawg
	 *  - arguments only contains constants
	 * @param arguments
	 */
	public void add(List<LETTER> arguments);

	/**
	 * Add all points of a given Dawg to this Dawg
	 * Requires:
	 *  - dawg's arities must be equal
	 * @param dawg
	 */
//	public void addAll(IDawg<LETTER, COLNAMES> dawg);
	
	/**
	 * Returns a new Dawg that recognizes the union language of this dawg and the
	 * argument Dawg.
	 */
	public IDawg<LETTER, COLNAMES> union(IDawg<LETTER, COLNAMES> other);

	public boolean isEmpty();

	public boolean isUniversal();

	/**
	 * same as join??
	 * @param fp
	 * @return
	 */
	public IDawg<LETTER, COLNAMES> intersect(IDawg<LETTER, COLNAMES> other);
	
	public IDawg<LETTER, COLNAMES> difference(IDawg<LETTER, COLNAMES> other);

//	public void removeAll(IDawg<LETTER, COLNAMES> other);

	/**
	 * Corresponds to the "superset or equal" set operation.
	 * @param points
	 * @return
	 */
	public boolean supSetEq(IDawg<LETTER, COLNAMES> points);

	/**
	 * Return a dawg where only the points are selected that match the given mapping.
	 * The mappings says for some columns what value they must have.
	 * (this is a special version of the normal select operation sigma_phi, where phi has the form x=a, 
	 *  for a column name x and a letter a)
	 *  Note that this does no project operation. The signature of the output dawg is the same as the 
	 *  signature of the input dawg (this Dawg).
	 * 
	 * @param selectMap restricts some COLNAMES in the signature to only one LETTER
	 * @return
	 */
	public IDawg<LETTER, COLNAMES> select(Map<COLNAMES, LETTER> selectMap);

	/**
	 * 
	 * @return true iff the language of this dawg contains exactly one element
	 */
	public boolean isSingleton();

	/**
	 * 
	 * @param translation Maps colnames of the predicate signature to colnames of the clause signature and constants
	 * @param targetSignature the signature of the clause
	 * @return
	 */
	public IDawg<LETTER, COLNAMES> translatePredSigToClauseSig(
			Map<COLNAMES, COLNAMES> translationColnameToColname,
			Map<COLNAMES, LETTER> translationColnameToLetter,
			SortedSet<COLNAMES> targetSignature);

	/**
	 * 
	 * @param translation maps colnames of the clause signature to colnames in the  predicate's signature
	 * @param argList the arguments of the ClauseLiteral that has the predicate whose signature we want to translate to
	 * @param newSignature the predicate's signature
	 * @return
	 */
	public IDawg<LETTER, COLNAMES> translateClauseSigToPredSig(
			Map<COLNAMES, COLNAMES> translation, List<Object> argList, SortedSet<COLNAMES> newSignature);

}
