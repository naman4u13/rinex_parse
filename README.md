# rinex_parse
GNSS data processing software

## Table Of Contents
1. [Description](#description)
2. [Current Version](#current-version)
    * [Input Files/Parser](#parser)
    * [Position Estimation Algorithms](#position-estimation-algorithms)
    * [Helper](#helper)
    * [Utility](#utility)
    * [Models](#models)
3. [A Note on IGS products/Error correction models](#note)
4. [Results](#results)
    * [How to interpret the Result Graph](#how-to-interpret-the-result-graph)
    * [Position Error Plots/Graphs and Results](#plots)
5. [Future Work](#future-work)
   

## Description
The 'rinex_parse' application is a GNSS data processing tool written in Java, It started out as a utility program to parse various IGS data and product file but eventually developed into a complete processing tool. Please note that the project uptill now has been developed only for personal R&D use and therefore is not ready to be consumed either as API, Service or Library. Still the modules/files responsible for a particular functionality are generally well written and can be used for reference. In case of any queries, please feel free to reach out.  

## Current Version
Current version of the application can process on multi-constellation, multi-frequency data to estimate code based position solution. Only Standard Point Positioning(SPP) is supported. Following subheadings define the overall structure of the codebase - 

### Input Files/Parser<a name="parser"></a>
Following files can be parsed and processed
1. IGS Data - RINEX 3.03 Observation and Navigation files
2. IGS Product - 
    1. SINEX
    2. SINEX-BIAS or BSX or DCB product files
    3. Global Ionospheric Maps(GIM) IONEX files
    4. Precise Orbit(.sp3) and Clock(.clk) product file
    5. Antenna/ANTEX file 
    6. EGNOS EMS file
3. Android GNSS Log file

Concerned [directory](https://github.com/naman4u13/rinex_parse/tree/master/src/main/java/com/RINEX_parser/fileParser)

### Position Estimation Algorithms
1. Least Squares Regression
2. Weighted Least Squares Regression - weights are assigned based on Elevation angle and SNR value
3. Extended Kalman Filter - current version of the App only supports Static Standard Point Positioning(SPP) 

Incase of Dual Frequency data, Iono-Free combination of obseravbles are used.

Singular Value Decomposition(SVD) is supported incase the `Y = AX + b` equation contains infinitely many solutions.

Concerned [directory](https://github.com/naman4u13/rinex_parse/tree/master/src/main/java/com/RINEX_parser/ComputeUserPos)

### Helper
1. Compute Elevation and Azimuth Angle of a Satellite
2. Compute Ionospheric correction based on Klobuchlar model
3. Compute Ionospheric Pierce Point
4. Compute Satellite position and velocity, and Satellite clock offset and drift.
5. Compute Troposphere correction based on UNB3 model(Saastamonien Model based Zenith Delay and Niell Mapping function)
6. Perform Carrier and Doppler smoothing
7. Assess the Integrity of the GNSS solution using RAIM algorithm
8. Perform Cycle Slip(CS) detection - 
    1. Dual Frequency(DF) CS detection based on [Geometry-Free](https://gssc.esa.int/navipedia/index.php/Detector_based_in_carrier_phase_data:_The_geometry-free_combination) and   [Melbourne-Wubenna](https://gssc.esa.int/navipedia/index.php/Detector_based_in_code_and_carrier_phase_data:_The_Melbourne-W%C3%BCbbena_combination) algorithm.
    2. Single Frequency(SF) CS detection based on [Code-Phase difference](https://gssc.esa.int/navipedia/index.php/Examples_of_single_frequency_Cycle-Slip_Detectors) and [Doppler-Aided](https://link.springer.com/article/10.1007/s10291-020-00993-0) algorithm.

Concerned [directory](https://github.com/naman4u13/rinex_parse/tree/master/src/main/java/com/RINEX_parser/helper)

### Utility
1. ECEF coordinates to Geodetic Latitude and Longitude
2. Time conversion between UTC time and GPS time
3. Calculate Haversine Distance
4. Lagrange Interpolator
5. Transformation from ENU to ECEF coordinates 
Concerned [directory](https://github.com/naman4u13/rinex_parse/tree/master/src/main/java/com/RINEX_parser/utility)

### Models
POJO or Java Model Classes.

Concerned [directory](https://github.com/naman4u13/rinex_parse/tree/master/src/main/java/com/RINEX_parser/models) 

## A Note on IGS products/Error correction models<a name="note"></a>
1. SINEX files are used to compare the accuracy of the estimated position solutions. They are also used to derive Reciever's Antenna Reference Point(ARP) and Phase Centre Offset(PCO), incase RINEX Observation file does not specify them.
2. DCB product file are used to derive to Satellite Code based Instrumental Bias or Inter-Signal Correction(ISC)
3. [IONEX/GIM](https://github.com/naman4u13/rinex_parse/blob/master/src/main/java/com/RINEX_parser/fileParser/IONEX.java), precise [Orbit](https://github.com/naman4u13/rinex_parse/blob/master/src/main/java/com/RINEX_parser/fileParser/Orbit.java) and [Clock](https://github.com/naman4u13/rinex_parse/blob/master/src/main/java/com/RINEX_parser/fileParser/Clock.java) parser files are defined as a class and its object serve the function of both parsing as well performing interpolation to compute required parameter. 
4. [Antenna/ANTEX](https://github.com/naman4u13/rinex_parse/blob/master/src/main/java/com/RINEX_parser/fileParser/Antenna.java) parser class is used to compute Satellite Phase Center as well as Carrier Phase Windup correction.
5. [Satellite Position computation](https://github.com/naman4u13/rinex_parse/blob/master/src/main/java/com/RINEX_parser/helper/ComputeSatPos.java) class/module also computes ECI coordinates of Satellite which helps in handling of Sagnac Effect.


## Results

Summary of observation model and data processing strategies. The results are for SPP method. 

| Item | Description |
| --- | ----------- |
| Station Code | IISC |
| Date Span | 2:00 AM - 10:00 PM, April 9, 2020 |
| Signal Selection | GPS L1 C/A (primary for Single Frequency estimation), GPS L2C(L) (used in dual frequency) |
| Sampling interval | 30 s |
| Elevation Cutoff | 15 degree |
| Time System | GPS time |
| Satellite Orbit and Clock | Final IGS product with 15 min and 30 sec sampling |
| Interpolation order for IGS Satellite Orbit product | 10 |
| Tropospheric delay model | UNB3 |
| Ionospheric delay model | IGS provided Global Ionospheric Map(GIM) |
| Satellite PCO and PCV | Antex - igs14.atx |
| TGD/DCB correction | CAS DCB(.BSX) file |
| Sagnac Effect | Calculated and used Satellite's ECI coordinates to account for Sagnac |
| Reciever ARP and PCO | RINEX observation file and SINEX file |
| Station reference coordinates| IGS SINEX solution file |
| Estimation Method | LeastSquare, Weighted LeastSquare, Extended Kalman Filter |

### How to interpret the Result Graph 
1. Following are the abbreviations - Extended Kalman Filter(EKF), Least Squares(LS), Weighted Least Squares(WLS), Single-Frequency(SF) and Dual Frequency(DF). 
2. Legend are as follows - "**LL off**" means Haversine Distance( horizontal/2D distance offset),  "**ECEF off**" means ECEF offset(3D distance offset)
3. Y-axis is in meter scale
4. Ignore "**TropoCorr**" term in the Legend

### Position Error Plots/Graphs and Results<a name="plots"></a>

<img src="https://github.com/naman4u13/rinex_parse/blob/master/imgs/IISC_singlefreq_regression.png" alt="image" title="a. LS and WLS based SF SPP solution" >
<img src="https://github.com/naman4u13/rinex_parse/blob/master/imgs/IISC_singlefreq_EKF.png" alt="image"  title="b. EKF based SF SPP solution"> 
<img src="https://github.com/naman4u13/rinex_parse/blob/master/imgs/IISC_dualfreq_EKF.png" alt="image"  title="c. EKF based DF SPP solution"> 

RMS table
| Estimator | 3D-RMS(in meter) |2D-RMS(in meter)|
| -------- | ---- | ---- |
| SF LS | 2.704 |1.220|
| SF WLS | 1.194 |0.640|
| SF EKF |0.643|0.558|
|DF EKF |0.727|0.299|

Converged Value at the end of data series
| Kalman Estimator | 3D-converged value(in meter) |2D-converged value(in meter)|
| -------- | ---- | ---- |
| SF EKF |0.506|0.423|
|DF EKF |0.763|0.181|

The correctness/accuracy of the programmed algorithm has been validated by processing on multiple other IGS station Observation file, the 3D and 2D error range for other station data has been more or less same, under a meter, with 2D error being naturally significanlty less than 3D error. Algorithm performance follows EKF>WLS>LS. 


## Future Work

1. Immediate goal is to integrate Carrier Phase measurements and develop SF/DF PPP algorithm, specifically [Phase-Adjusted PPP](https://gnss.curtin.edu.au/wp-content/uploads/sites/21/2016/04/Le2006Recursive.pdf).  
2. Complete and consolidate multi-frequency, multi-constellation feature of the application.
3. Advance from solving ambiguities in "Float" domain towards resolving ambiguities in "Integer" domain. Implement advance techniques like PPP-AR and PPP-RTK(SSR).
4. Migrate to Android Platform and integrate GNSS Raw Measurements API, and build an Android Application capable of providing Multi-Constellation, multi-frequency Kinematic PPP position solution with centimeter level accuracy.
5. Explore utility of Doppler measurements and integrate INS data and various other sensors data in positioning domain.
