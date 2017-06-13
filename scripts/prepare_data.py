# import numpy as np
import re
import os
import random
import numpy as np
from scipy.sparse import csr_matrix


def load_data(FILEIN):
    """ return compressed sparse row matrix,
            max_rowID,
            max_columnID,
            minValue,
            maxValue"""
    fi = open(FILEIN, 'r')
    indx_ufactor = []
    indx_ifactor = []
    value_ifactor = []

    for line in fi:
        elems = re.split("\\s+|:+", line)
        indx_ufactor.append(elems[0].strip())
        indx_ifactor.append(elems[1].strip())
        value_ifactor.append(elems[2].strip())

    vs = np.array(value_ifactor, dtype=np.float)
    rs = np.array(indx_ufactor, dtype=np.int32)
    cs = np.array(indx_ifactor, dtype=np.int32)
    sm = csr_matrix((vs, (rs, cs)))
    return sm, np.amax(rs), np.amax(cs), np.amin(vs), np.amax(vs)


def uniformly_split_data(FILEOUT, sampRato, sm,
                         max_rid, max_cid, minVal, maxVal):
    if not os.path.exists(FILEOUT):
        os.makedirs(FILEOUT)
    fo_train = open(FILEOUT + 'trainingset', 'w')
    fo_test = open(FILEOUT + 'testingset', 'w')
    fo_conf = open(FILEOUT + 'dConfig.properties', 'w')

    num_row_train = 0
    num_val_train = 0
    num_row_test = 0
    num_val_test = 0

    shape = sm.shape
    for r in range(shape[0]):
        row = sm.getrow(r)
        if row.getnnz() == 0:
            continue
        if r % 500 == 0:
            print(r)

        feat_train = ''
        count_train = 0
        feat_test = ''
        count_test = 0

        rnz = row.nonzero()
        for rl, cl in zip(rnz[0], rnz[1]):
            if random.random() <= sampRato:
                feat_train += ' {0}:{1}'.format(cl, row[rl, cl])
                count_train += 1
            else:
                feat_test += ' {0}:{1}'.format(cl, row[rl, cl])
                count_test += 1

        if count_train != 0:
            num_row_train += 1
            num_val_train += count_train + 1
            fo_train.write('0 0 1 {0} {1}:1{2}\n'.format(
                count_train, r, feat_train))
        if count_test != 0:
            num_row_test += 1
            num_val_test += count_test + 1
            fo_test.write('0 0 1 {0} {1}:1{2}\n'.format(
                count_test, r, feat_test))
    fo_conf.write(
        """$USER_COUNT_VALUE={0}
        $ITEM_COUNT_VALUE={1}
        $MAX_RATING_VALUE={2}
        $MIN_RATING_VALUE={3}

        $TRAIN_ROW_NUM_VALUE={4}
        $TRAIN_VAL_NUM_VALUE={5}

        $TEST_ROW_NUM_VALUE={6}
        $TEST_VAL_NUM_VALUE={7}"""
        .format(max_rid, max_cid, maxVal, minVal,
                num_row_train, num_val_train, num_row_test, num_val_test))

    fo_train.close()
    fo_test.close()
    fo_conf.close()


if __name__ == '__main__':
    FILEIN = 'C:/Dataset/ml-1m/ratings.dat'
    print('load data: {0}'.format(FILEIN))
    sm, max_rid, max_cid, minVal, maxVal = load_data(FILEIN)

    sampRato = 0.9
    for num_splits in range(1, 5):
        FILEOUT = 'C:/Dataset/ml-1m/{0}/'.format(num_splits)
        print('Split-{0}:{1}'.format(num_splits, FILEOUT))
        uniformly_split_data(FILEOUT, sampRato, sm,
                             max_rid + 1, max_cid + 1, minVal, maxVal)
