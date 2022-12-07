# Auxiliary Material for Information Systems Manuscript "Reconciling the Trade-off between Prediction Accuracy and Earliness in Prescriptive Business Process Monitoring"

## Experimental Outcomes
- [Summary spreadsheet (XLSX)](ExperimentalOutcomes/summaries/isj-comparative-all-alternatives.xlsx) that consolidates the results on various levels of abstraction. It also includes additional information about the event log data sets used.
- [Charts data (CSV)](ExperimentalOutcomes/charts/) that contains the data used for the charts presented in the paper.

## Code
- [Executable (JAR)](Executables/isj-generic.jar) and [Code (Java)](Code/experiment-execution/) to execute the experiments. Requires Requires `commons-lang3-3.12.0.jar` and `opencsv-5.6`, as well as prediction results and Online RL results as input (can be downloaded from below). The three mandatory command line parameters are: 
  - `data-set`: 1 = BPIC12, 2 =BPIC17, 3 = Traffic, 4 = Cargo
  - `prediction-model`: 1 = LSTM; 2 = RF
  - `research-question`: 1 = RQ1: savings (output on stdout); 2 = RQ2: non-stationarity (output in directory ./out)

- [Code (Python)](URL) for Online RL approach (TO BE UPLOADED)

## Prediction Results
The prediction results serve as input for the experiment execution code.
- [LSTM data sets (CSV)](https://uni-duisburg-essen.sciebo.de/s/35iFroyyxMqf1tc). They need to be stored in a directory called `./_rnn`. 
- [RF data sets (CSV)](https://uni-duisburg-essen.sciebo.de/s/VAt5IgBjO4WAZUN). They need to be stored in a directory called `./_rf`. 

## Online RL Results
The Online RL results serve as input for the experiment execution code. 
They need to be stored in a directory called 
`./_prescriptions`. The filenames are `NonPropheticCuriosityAdaptationPunish_<PredModel><DataSet><NbrOfRun>`, with `<PredModel>` = `""` for LSTM and `<PredModel>` = `20` for RF, as well as `<DataSet>` = `10` for BPIC12, `20` for BPIC17, `30` for Cargo, and `40` for Traffic.
- [Online RL results (ZIPped CSV)](https://uni-duisburg-essen.sciebo.de/s/I8ELo2N6jNW2xFe)

## Event Log Data Sets
The event log data sets are offered in two formats: 

- [Data sets (CSV)](https://uni-duisburg-essen.sciebo.de/s/geuui7pDuqms15E) converted to serve as input for training the prediction models.

- Links to original source for data sets: [BPIC12](https://data.4tu.nl/articles/dataset/BPI_Challenge_2012/12689204), [BPIC17](https://data.4tu.nl/articles/dataset/BPI_Challenge_2017/12696884), [Traffic](https://data.4tu.nl/articles/dataset/Road_Traffic_Fine_Management_Process/12683249), and [Cargo](https://archive.ics.uci.edu/ml/datasets/Cargo+2000+Freight+Tracking+and+Tracing)




