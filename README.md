# StableMA
<h3>[Read Me]</h3>
This code implemented:
<p>
  <ol type="1">
  <li>
    <strong>Regularized SVD</strong>.
    Arkadiusz Paterek. 
    Improving regularized singular value decompositionfor collaborativefiltering. 
    In KDD CUP, 2007.
  </li>
  
  <li>
    <strong>Group Sparsity Matrix Factorization</strong>.
    Ting Yuan, Jian Cheng, Xi Zhang, Shuang Qiu, Hanqing Lu. 
    Recommendation by mining multiple user behaviors with group sparsity. 
    In AAAI, 2014.
  </li>

  <li>
    <strong>Stable Matrix Approximation</strong>.
    Dongsheng Li, Chao Chen, Qin Lv, Junchi Yan, Li Shang, Stephen Chu.
    Low-Rank Matrix Approximation with Stability.
    In ICML, 2016.
  </li>
  
  <li>
    <strong>WEMAREC</strong>.
    Chao Chen, Dongsheng Li, Yingying Zhao, Qin Lv, Li Shang.
    WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation.
    In SIGIR, 2015.
  </li>
  </ol>
</p>


<h3>[Import Guidance]</h3>
<p>
  Please use the lastest Eclipse Mar to import this project,
  and note that the dependencies are managed by Maven Plug-in.
  Therefore, if you had dependency issues, please use Maven to import this project again.
</p>

<h3>[Running Suggestion]</h3>
<p>
  <ol type="1">
  <li>
        We upload a workable MovieLens-10M and 20M data-set, which can be downloaded from 
  	<a href="https://drive.google.com/open?id=0Bz4myK9f22j4NUpUMFZQb0pidm8">ML-10M</a> or
	<a href="https://drive.google.com/open?id=0Bz4myK9f22j4a3Q3cTdwcUUtVEU">ML-20M</a>.
  </li>
  <li>
  	If you upzip one dataset in the path "C:/Dataset/", 
  	please set the variable "$ROOT_DIR_ARR=C:/Dataset/1/" 
  	in the config file under folder "src/main/resources/samples".
  </li>
  <li>
  	Run the test cases under folder "src/test/java".
  </li>
  </ol>
</p>
