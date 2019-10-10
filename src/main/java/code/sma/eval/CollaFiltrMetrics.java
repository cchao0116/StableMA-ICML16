package code.sma.eval;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.model.Model;

/**
 * Metrics for Collaborative filtering method
 * 
 * @author Chao.Chen
 * @version $Id: CollaFiltrMetrics.java, v 0.1 Jul 5, 2017 10:13:43 AM$
 */
public class CollaFiltrMetrics extends EvaluationMetrics {

	public CollaFiltrMetrics() {
	}

	public CollaFiltrMetrics(int N) {
		this.N = N;
	}

	/**
	 * compute all the evaluations
	 * 
	 * @param ttMatrix
	 *            test data
	 */
	public void evalRating(Model model, AbstractIterator idata) {
		// Rating Prediction evaluation
		mae = 0.0d;
		mse = 0.0d;

		// Ranking Prediction evaluation
		precision = 0.0d;
		recall = 0.0d;
		hitrate = 0.0d;
		ndcg = 0.0d;

		double[] dcg = new double[this.N];
		{
			double normalize = 0.0d;
			double idcg = 0.0d;
			for (int n = 0; n < this.N; n++) {
				idcg = 1.0 / (Math.log(n + 2) / Math.log(2));
				dcg[n] = idcg;
				normalize += idcg;
			}

			for (int n = 0; n < this.N; n++) {
				idcg = dcg[n];
				dcg[n] = idcg / normalize;
			}
		}

		idata = idata.clone();
		int nnz = 0;
		int nnu = 0;
		while (idata.hasNext()) {
			DataElem e = idata.next();
			short num_ifactor = e.getNum_ifacotr();
			if (num_ifactor == 0) {
				continue;
			}

			// rating related measures
			nnz += num_ifactor;
			double[] ratings = model.predict(e);
			for (int f = 0; f < num_ifactor; f++) {
				double realVal = e.getValue_ifactor(f);
				double predVal = ratings[f];

				mae += Math.abs(realVal - predVal);
				mse += Math.pow(realVal - predVal, 2.0d);
			}

			if(this.N <=0) continue;
			// ranking related measures
			nnu += 1;
			int[] rankings = model.ranking(e);
			double ihit = 0.0d;
			double idcg = 0.0d;
			double hits = 0.0d;
			for (int n = 0; n < this.N; n++) {
				ihit = rankings[n];
				hits += ihit;
				idcg += ihit == 0.0 ? 0.0 : dcg[n];
			}
			precision += hits / this.N;
			recall += hits / e.getNum_ifacotr();
			hitrate += hits == 0.0 ? 0.0 : 1.0;
			ndcg += idcg;
		}

		mae /= nnz;
		mse /= nnz;

		if (this.N > 0) {
			precision /= nnu;
			recall /= nnu;
			hitrate /= nnu;
			ndcg /= nnu;
		}
	}

}
