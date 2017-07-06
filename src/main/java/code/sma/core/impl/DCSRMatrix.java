package code.sma.core.impl;

import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

import code.sma.core.DataElem;
import code.sma.util.StringUtil;
import it.unimi.dsi.fastutil.chars.Char2FloatMap;
import it.unimi.dsi.fastutil.floats.Float2CharMap;

/**
 * storage block of random order 'DISCRETE' input, 
 * in CSR format for sparse matrix, multiple lines are stored together in a block
 * 
 * @author Chao.Chen
 * @version $Id: DCSRMatrix.java, v 0.1 Jul 5, 2017 7:47:19 PM$
 */
public class DCSRMatrix extends CSRMatrix {
    // For discrete data
    /** array of char*/
    private char[]          dscrt_feat_value;
    /** MAP: char 2 float value*/
    protected Char2FloatMap char2num;
    protected Float2CharMap num2char;

    public DCSRMatrix(int num_row, int num_val, Char2FloatMap char2num, Float2CharMap num2char) {
        super();

        row_label = new float[num_row];
        row_ptr = new int[3 * num_row + 1];

        feat_index = new int[num_val];

        dscrt_feat_value = new char[num_val];
        this.char2num = char2num;
        this.num2char = num2char;
    }

    /** 
     * @see code.sma.core.impl.CSRMatrix#loadNext(java.lang.String)
     */
    @Override
    public void loadNext(String line) {
        assert StringUtil.isNotBlank(line) : "Line must not be blank";

        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(":+|\\s+");

        row_label[num_row] = scanner.nextFloat();

        int num = scanner.nextInt();
        this.num_global += num;
        row_ptr[3 * num_row + 1] = row_ptr[3 * num_row] + num;

        num = scanner.nextInt();
        this.num_ufactor += num;
        row_ptr[3 * num_row + 2] = row_ptr[3 * num_row + 1] + num;

        num = scanner.nextInt();
        this.num_ifactor += num;
        row_ptr[3 * num_row + 3] = row_ptr[3 * num_row + 2] + num;

        while (scanner.hasNextInt()) {
            feat_index[num_val] = scanner.nextInt();
            dscrt_feat_value[num_val] = num2char.get(scanner.nextFloat());
            num_val++;
        }

        num_row++;
        IOUtils.closeQuietly(scanner);
    }

    /** 
     * @see code.sma.core.impl.CSRMatrix#setValueRefVec(code.sma.core.impl.CRefVector, int, int)
     */
    @Override
    protected void setValueRefVec(CRefVector refvec, int offset, int num_factors) {
        refvec.setCharPtr(dscrt_feat_value);
        refvec.setPtr_offset(offset);
        refvec.setNum_factors(num_factors);
    }

    /** 
     * @see code.sma.core.impl.CSRMatrix#iterator()
     */
    @Override
    public Iterator<DataElem> iterator() {
        return new CharIter();
    }

    protected class CharIter extends Iter {
        CharIter() {
            super();
            e.setValue_global(new CPrjRefVector((char[]) null, 0, 0, char2num));
            e.setValue_ufactor(new CPrjRefVector((char[]) null, 0, 0, char2num));
            e.setValue_ifactor(new CPrjRefVector((char[]) null, 0, 0, char2num));
        }
    }
}
