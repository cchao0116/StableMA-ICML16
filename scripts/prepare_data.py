import re
import os
import sys
import random
import numpy as np
from scipy.sparse import lil_matrix

NUM_MAX_ROWS = 1000 * 1000
NUM_MAX_COLUMNS = 1000 * 1000


def load_data(FILEIN):
    """ return compressed sparse row matrix,
            max_rowID,
            max_columnID,
            minValue,
            maxValue"""
    fi = open(FILEIN, 'r')
    sm = lil_matrix((NUM_MAX_ROWS, NUM_MAX_COLUMNS), dtype=np.float16)

    num_row = 0
    num_col = 0
    max_val = np.finfo(np.float16).min
    min_val = np.finfo(np.float16).max
    for line in fi:
        elems = re.split("\\s+|:+", line)
        u = int(elems[0].strip())
        i = int(elems[1].strip())
        v = np.float16(elems[2].strip())
        sm[u, i] = v
        num_row = max(num_row, u)
        num_col = max(num_col, i)
        max_val = max(max_val, v)
        min_val = min(min_val, v)

    num_row += 1
    num_col += 1
    return sm, num_row, num_col, max_val, min_val


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

    discrete_set = set([])

    for r in range(max_rid):
        row = sm.getrowview(r)
        if row.getnnz() == 0:
            continue
        if r % 500 == 0:
            print(r)

        feat_train = ''
        count_train = 0
        feat_test = ''
        count_test = 0

        # randomly split the data into training and testing data
        rnz = row.nonzero()
        for rl, cl in zip(rnz[0], rnz[1]):
            if random.random() <= sampRato:
                feat_train += ' {0}:{1}'.format(cl, row[rl, cl])
                count_train += 1
            else:
                feat_test += ' {0}:{1}'.format(cl, row[rl, cl])
                count_test += 1

            if len(discrete_set) <= 260:
                discrete_set.add(row[rl, cl])

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

    # configure file
    fo_conf.write(
        """$USER_COUNT_VALUE={0}
        $ITEM_COUNT_VALUE={1}
        $MAX_RATING_VALUE={2}
        $MIN_RATING_VALUE={3}

        $TRAIN_ROW_NUM_VALUE={4}
        $TRAIN_VAL_NUM_VALUE={5}

        $TEST_ROW_NUM_VALUE={6}
        $TEST_VAL_NUM_VALUE={7}\n""".format(max_rid, max_cid, maxVal, minVal,
                                            num_row_train, num_val_train,
                                            num_row_test, num_val_test))

    # optimize for discrete input
    len_ds = len(discrete_set)
    if len_ds <= 256:
        ds_str = str(discrete_set)
        len_ds = len(ds_str)
        fo_conf.write('$DISCRETE_INPUT_SET={0}'.format(ds_str[1:len_ds - 1]))

    fo_train.close()
    fo_test.close()
    fo_conf.close()


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print('Please input the path of orignial ML-10M(1M)/Neflix Data')
        exit(1)

    ROOT_DIR = sys.argv[1]
    if os.path.isfile(ROOT_DIR):
        FILEIN = ROOT_DIR
        ROOT_DIR = '{0}/'.format(os.path.dirname(FILEIN))
    else:
        FILEIN = '{0}ratings.dat'.format(ROOT_DIR)

    print('load data: {0}'.format(FILEIN))
    sm, max_row, max_col, minVal, maxVal = load_data(FILEIN)

    sampRato = 0.9
    for num_splits in range(1, 6):
        FILEOUT = '{0}{1}/'.format(ROOT_DIR, num_splits)
        print('Split-{0}:{1}'.format(num_splits, FILEOUT))
        uniformly_split_data(FILEOUT, sampRato, sm,
                             max_row, max_col, minVal, maxVal)
