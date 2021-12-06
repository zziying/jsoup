package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Base combining (and, or) evaluator.
 */
public abstract class CombiningEvaluator extends Evaluator {
    final ArrayList<Evaluator> evaluators;
    int num = 0;

    CombiningEvaluator() {
        super();
        evaluators = new ArrayList<>();
    }

    CombiningEvaluator(Collection<Evaluator> evaluators) {
        this();
        this.evaluators.addAll(evaluators);
        updateNumEvaluators();
    }

    @Nullable Evaluator rightMostEvaluator() {
        return num > 0 ? evaluators.get(num - 1) : null;
    }
    
    void replaceRightMostEvaluator(Evaluator replacement) {
        evaluators.set(num - 1, replacement);
    }

    void updateNumEvaluators() {
        // used so we don't need to bash on size() for every match test
        num = evaluators.size();
    }

    public static final class And extends CombiningEvaluator {
        And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        /**
         * Check if the two element matches. 
         * 
         * @param root One of the element we want to check.
         * @param node Another element we would like to check.
         * @return return a boolean indicates whether these two elements matches.
         * */
        @Override
        public boolean matches(Element root, Element node) {
        	boolean result = false;
        	List<Evaluator> accumulated = new ArrayList<Evaluator>();
        	// check whether we should use accumulated array
        	String elementText = node.outerHtml();
        	String endTag = elementText.substring(elementText.length() - node.tagName().length() - 3, elementText.length());
        	if (endTag.contains(".")) {
            	accumulated.add(evaluators.get(num - 1));
            	for (int i = num - 2; i >= 0; i--) {
            		Evaluator s = evaluators.get(i);
            		Evaluator lastElement = accumulated.get(accumulated.size() - 1);
            		accumulated.add(s.append(lastElement));
            	}
        	}
            for (int i = num - 1; i >= 0; i--) { // process backwards so that :matchText is evaled earlier, to catch parent query. todo - should redo matchText to virtually expand during match, not pre-match (see SelectorTest#findBetweenSpan)
                Evaluator s = evaluators.get(i);
                Evaluator a = null;
                if (accumulated.size() != 0) {
                	a = accumulated.get(i);
                }
                if (s.matches(root, node)){
                	result = true;
                	break;
                } else if (a != null && a.matches(root, node)){
                	result = true;
                	break;
                } else {
                	result = false;
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, "");
        }

        /**
         * Return a self appended evaluator.
         * @param e another Evaluator
         * @return a new generated Evaluator
         * */
		@Override
		public Evaluator append(Evaluator e) {
			// TODO Auto-generated method stub
        	return e;
		}
    }

    public static final class Or extends CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        Or(Collection<Evaluator> evaluators) {
            super();
            if (num > 1)
                this.evaluators.add(new And(evaluators));
            else // 0 or 1
                this.evaluators.addAll(evaluators);
            updateNumEvaluators();
        }

        Or(Evaluator... evaluators) { this(Arrays.asList(evaluators)); }

        Or() {
            super();
        }

        public void add(Evaluator e) {
            evaluators.add(e);
            updateNumEvaluators();
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (s.matches(root, node))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, ", ");
        }

        /**
         * Return a self appended evaluator.
         * @param e another Evaluator
         * @return a new generated Evaluator
         * */
		@Override
		public Evaluator append(Evaluator e) {
			return e;
		}
    }
}
