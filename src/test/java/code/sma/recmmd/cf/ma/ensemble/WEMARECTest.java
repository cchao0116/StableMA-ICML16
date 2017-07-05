package code.sma.recmmd.cf.ma.ensemble;

/**
 * 
 * @author Chao.Chen
 * @version $Id: WEMARECTest.java, v 0.1 2017年6月13日 下午1:03:19 Chao.Chen Exp $
 */
public class WEMARECTest extends AbstractEnsTest {

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.AbstractTest#getConfig()
     */
    @Override
    protected String getConfig() {
        return "src/main/resources/samples/WEMAREC.properties";
    }

}
