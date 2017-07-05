package code.sma.recmmd.tree;

import code.sma.recmmd.standalone.AbstractTest;

/**
 * 
 * @author Chao.Chen
 * @version $Id: GBDT.java, v 0.1 Jul 4, 2017 2:10:23 PM $
 */
public class GBDT extends AbstractTest {

    /**
     * @see code.sma.recmmd.standalone.AbstractTest#getConfig()
     */
    @Override
    protected String getConfig() {
        return "src/main/resources/samples/GBM.properties";
    }

}
