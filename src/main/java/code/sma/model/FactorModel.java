package code.sma.model;

import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.MinMaxPriorityQueue;

import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.util.LoggerUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: FactorModel.java, v 0.1 2017年6月19日 下午2:47:58 Chao.Chen Exp $
 */
public class FactorModel extends AbstractModel {
	private static final long serialVersionUID = 1L;

	public int topN;
	protected transient MinMaxPriorityQueue<Pair<Integer, Double>> top_pq;

	/** User profile in low-rank matrix form. */
	public DenseMatrix ufactors;
	/** Item profile in low-rank matrix form. */
	public DenseMatrix ifactors;
	/** User-dependent bias */
	public DenseVector ubias;
	/** Item-dependent bias */
	public DenseVector ibias;
	/** the base and stationary prediction */
	public float base;

	public FactorModel(Configures conf) {
		super(conf);

		int userCount = conf.getInteger("USER_COUNT_VALUE");
		int itemCount = conf.getInteger("ITEM_COUNT_VALUE");
		int featureCount = conf.getInteger("FEATURE_COUNT_VALUE");
		this.topN = conf.getInteger("TOPN_VALUE");
		this.top_pq = MinMaxPriorityQueue.orderedBy(new Comparator<Pair<Integer, Double>>() {
			@Override
			public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
				return (int) Math.signum(o2.getRight() - o1.getRight());
			}
		}).maximumSize(this.topN).create();

		this.ufactors = new DenseMatrix(userCount, featureCount);
		this.ifactors = new DenseMatrix(itemCount, featureCount);
		this.ubias = new DenseVector(userCount);
		this.ibias = new DenseVector(itemCount);
		this.base = conf.containsKey("BASE_PREDICTION_VALUE") ? conf.getFloat("BASE_PREDICTION_VALUE") : 0.0f;
	}

	public FactorModel(Configures conf, DenseMatrix ufactors, DenseMatrix ifactors) {
		super(conf);

		int userCount = conf.getInteger("USER_COUNT_VALUE");
		int itemCount = conf.getInteger("ITEM_COUNT_VALUE");

		this.ufactors = ufactors;
		this.ifactors = ifactors;
		this.ubias = new DenseVector(userCount);
		this.ibias = new DenseVector(itemCount);
		this.base = conf.containsKey("BASE_PREDICTION_VALUE") ? conf.getFloat("BASE_PREDICTION_VALUE") : 0.0f;
	}

	/**
	 * @see code.sma.model.Model#predict(int, int, code.sma.core.DataElem[])
	 */
	@Override
	public double predict(int u, int i, DataElem... e) {
		assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";
		AbstractVector ufs = ufactors.getRowRef(u);
		AbstractVector ifs = ifactors.getRowRef(i);

		if (ufs == null || ifs == null) {
			LoggerUtil.debug(runningLogger, String.format("null latent factors for (%d,%d)-entry", u, i));
			return defaultValue;
		} else {
			double prediction = base + ubias.floatValue(u) + ibias.floatValue(i) + ufs.innerProduct(ifs);
			return Math.max(minValue, Math.min(prediction, maxValue));
		}
	}

	/**
	 * @see code.sma.model.AbstractModel#predict(code.sma.core.DataElem)
	 */
	@Override
	public double[] predict(DataElem e) {
		assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";

		short num_ifactor = e.getNum_ifacotr();
		double[] preds = new double[num_ifactor];

		int u = e.getIndex_user(0);
		AbstractVector ufs = ufactors.getRowRef(u);
		for (int p = 0; p < num_ifactor; p++) {
			int i = e.getIndex_item(p);
			AbstractVector ifs = ifactors.getRowRef(i);

			if (ufs == null || ifs == null) {
				LoggerUtil.debug(runningLogger, String.format("null latent factors for (%d,%d)-entry", u, i));
				preds[p] = defaultValue;
			} else {
				double prediction = base + ubias.floatValue(u) + ibias.floatValue(i) + ufs.innerProduct(ifs);
				preds[p] = Math.max(minValue, Math.min(prediction, maxValue));
			}
		}
		return preds;
	}

	/**
	 * @see code.sma.model.AbstractModel#ranking(code.sma.core.DataElem)
	 */
	@Override
	public int[] ranking(DataElem e) {
		assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";

		// compute top-N recommendations
		this.top_pq.clear();
		int u = e.getIndex_user(0);
		AbstractVector ufs = ufactors.getRowRef(u);
		for (int i = 0; i < this.ifactors.shape()[0]; i++) {
			AbstractVector ifs = ifactors.getRowRef(i);
			if (ufs == null || ifs == null) {
				LoggerUtil.debug(runningLogger, String.format("null latent factors for (%d,%d)-entry", u, i));
				continue;
			} else {
				double score = base + ubias.floatValue(u) + ibias.floatValue(i) + ufs.innerProduct(ifs);
				this.top_pq.add(new MutablePair<Integer, Double>(i, score));
			}
		}

		// build hash set for ground-truth
		HashSet<Integer> ev_items = new HashSet<Integer>();
		short num_ifactor = e.getNum_ifacotr();
		for (int p = 0; p < num_ifactor; p++) {
			ev_items.add(e.getIndex_item(p));
		}

		// compute hit status for top-N recommendations
		int[] preds = new int[topN];
		for (int p = 0; p < this.topN; p++) {
			preds[p] = ev_items.contains(this.top_pq.poll().getLeft()) ? 1 : 0;
		}

		return preds;
	}

}
