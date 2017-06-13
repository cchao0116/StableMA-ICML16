package code.sma.recmmd.ensemble;

/**
 * 
 * @author Chao.Chen
 * @version $Id: MultTskREC.java, v 0.1 2017年2月28日 下午4:23:17 Chao.Chen Exp $
 */
public class MTRECTest extends AbstractEnsTest {

    /** 
     * @see code.sma.recmmd.ensemble.AbstractEnsTest#getConfig()
     */
    @Override
    protected String getConfig() {
        return "src/main/resources/samples/MTREC.properties";
    }

}
