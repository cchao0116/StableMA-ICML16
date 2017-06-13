import numpy as np
from pylab import plt
from  r_pca import R_pca
from numpy import linalg as LA

import os
import sys

Ds = []

fo = open(sys.argv[1], 'r')
for line in fo:
	line = line.strip()
	line = line[1: len(line) -1 ]
	d = line.split(',')
	Ds.append(np.array(d))
D = np.vstack(Ds)
D = D.astype(np.float)


rpca = R_pca(D)
L, S = rpca.fit(max_iter=10000, iter_print=100)
U, s, V = np.linalg.svd(L, full_matrices=False)

appxS = np.zeros(s.shape[0])
appxS[0] = s[0]
appxS = np.diag(appxS)
appL = np.dot(U, np.dot(appxS, V))
print(appL[0,:])