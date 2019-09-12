package code.sma.recmmd.cf.ma.ensemble;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.main.Configures;
import code.sma.plugin.Discretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.cf.ma.standalone.GLOMA;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * 
 * @author Chao.Chen
 * @version $Id: MultTskREC.java, v 0.1 2017年2月28日 下午12:32:54 Chao.Chen Exp $
 */
public class MultTskREC extends EnsembleFactorRecmmder {
	/** SerialVersionNum */
	protected static final long serialVersionUID = 1L;

	/*
	 * ======================================== 
	 * Model specific parameters
	 * ========================================
	 */
	/** the arrays containing random seeds */
	private Queue<Long> randSeeds;
	/** the sampling rate of randomized submatrix */
	private double samplingRate;

	/*
	 * ======================================== 
	 * Constructors
	 * ========================================
	 */
	public MultTskREC(Configures conf, Map<String, Plugin> plugins) {
		super(conf, plugins);
		runtimes.doubles = new DoubleArrayList(conf.getDoubleArr("BETA"));

		samplingRate = conf.getDouble("SAMPLE_RATE_VALUE");

		this.randSeeds = new LinkedList<Long>();
		{
			String[] randSds = ((String) conf.get("RANDOM_SEED_SET")).split(",|\t| ");
			for (String rand : randSds) {
				long seed = Long.valueOf(rand.trim());
				randSeeds.add(seed == -1 ? ((long) (Math.random() * Long.MAX_VALUE)) : seed);
			}
		}
	}

	/**
	 * @see code.sma.recmmd.cf.ma.ensemble.EnsembleFactorRecmmder#buildModel(code.sma.core.AbstractMatrix,
	 *      code.sma.core.AbstractMatrix)
	 */
	@Override
	public void buildModel(AbstractMatrix train, AbstractMatrix test) {
		Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
		double[][][] ensmbleWs = dctzr.cmpEnsmblWs((AbstractIterator) train.iterator());
		runtimes.ensmblUWs = ensmbleWs[0];
		runtimes.ensmblIWs = ensmbleWs[1];

		super.buildModel(train, test);
	}

	/**
	 * @see code.sma.thread.TaskMsgDispatcher#map()
	 */
	@Override
	public Object map() {
		long randSeed = -1;

		synchronized (MAP_MUTEX) {
			if (randSeeds.isEmpty()) {
				return null;
			} else {
				randSeed = randSeeds.poll().longValue();
			}
		}

		return ranMap(randSeed);
	}

	protected Object ranMap(long randSeed) {
		int userCount = runtimes.userCount;
		int itemCount = runtimes.itemCount;

		Random ran = new Random(randSeed);
		boolean[] raf = new boolean[userCount];
		for (int u = 0; u < userCount; u++) {
			if (ran.nextFloat() < samplingRate) {
				raf[u] = true;
			}
		}

		boolean[] caf = new boolean[itemCount];
		for (int i = 0; i < itemCount; i++) {
			if (ran.nextFloat() < samplingRate) {
				caf[i] = true;
			}
		}

		GLOMA rcmmd = new GLOMA(runtimes.conf, raf, caf, runtimes.plugins);
		synchronized (MultTskREC.class) {
			rcmmd.runtimes.threadId = runtimes.threadId++;
		}
		return rcmmd;
	}

	/**
	 * @see code.sma.recmmd.cf.ma.ensemble.EnsembleFactorRecmmder#ensnblWeight(int,
	 *      int, double)
	 */
	@Override
	public double ensnblWeight(int u, int i, double prediction) {
		int indx = ((Discretizer) runtimes.plugins.get("DISCRETIZER")).convert(prediction);
		return 1.0 + runtimes.doubles.getDouble(0) * runtimes.ensmblUWs[u][indx]
				+ runtimes.doubles.getDouble(1) * runtimes.ensmblIWs[i][indx];
	}

	/**
	 * @see code.sma.recmmd.Recommender#toString()
	 */
	@Override
	public String toString() {
		return String.format("MTREC%s_SR[%d]_Ens[%d_%d]", runtimes.briefDesc(), Math.round(samplingRate * 100),
				(int) (runtimes.doubles.getDouble(0) * 100), Math.round(runtimes.doubles.getDouble(1) * 100));
	}

	@Override
	public int[] ranking(DataElem e) {
		// TODO Auto-generated method stub
		return null;
	}

}
