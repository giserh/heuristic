package gui;

import com.bbn.openmap.dataAccess.shape.DbfTableModel;
import com.bbn.openmap.dataAccess.shape.EsriPolyline;
import com.bbn.openmap.dataAccess.shape.EsriPolylineList;
import com.bbn.openmap.dataAccess.shape.EsriShapeExport;
import com.bbn.openmap.omGraphics.OMGraphic;
import dataStore.DataInOut;
import dataStore.DataStorer;
import dataStore.Edge;
import dataStore.Sink;
import dataStore.Solution;
import dataStore.Source;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import solver.MPSWriter;
import solver.Solver;

import static utilities.Utilities.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import solver.Heuristic;

/**
 *
 * @author yaw
 */
public class ControlActions {

    private String basePath = "";
    private String dataset = "";
    private String scenario = "";

    private DataStorer data;
    private Solver solver;
    private ImageView map;
    private Pane sourceLocationsLayer;
    private Pane sinkLocationsLayer;
    private Pane sourceLabelsLayer;
    private Pane sinkLabelsLayer;
    private Pane shortestPathsLayer;
    private Pane candidateNetworkLayer;
    private Pane rawDelaunayLayer;
    private Pane solutionLayer;
    private TextArea messenger;
    private Gui gui;

    public ControlActions(ImageView map, Gui gui) {
        this.map = map;
        this.gui = gui;
    }

    public void toggleCostSurface(Boolean show, Rectangle background) {
        if (show) {

            Image img = new Image("file:" + data.getCostSurfacePath());
            map.setImage(img);

            // Adjust background.
            background.setWidth(map.getFitWidth());
            background.setHeight(map.getFitHeight());
        } else {
            map.setImage(null);
        }
    }

    public void selectDataset(File datasetPath, ChoiceBox scenarioChoice) {
        // Clear GUI
        gui.fullReset();

        this.dataset = datasetPath.getName();
        this.basePath = datasetPath.getParent();

        // Populate scenarios ChoiceBox.
        File f = new File(datasetPath, "Scenarios");
        ArrayList<String> dirs = new ArrayList<>();
        for (File file : f.listFiles()) {
            if (file.isDirectory() && file.getName().charAt(0) != '.') {
                dirs.add(file.getName());
            }
        }
        scenarioChoice.setItems(FXCollections.observableArrayList(dirs));
    }

    public void initializeDatasetSelection(ChoiceBox datasetChoice) {
        // Set initial datasets.
        File f = new File(basePath);
        ArrayList<String> dirs = new ArrayList<>();
        for (File file : f.listFiles()) {
            if (file.isDirectory() && file.getName().charAt(0) != '.') {
                dirs.add(file.getName());
            }
        }
        datasetChoice.setItems(FXCollections.observableArrayList(dirs));
    }

    public void selectScenario(String scenario, Rectangle background, ChoiceBox solutionChoice) {
        if (scenario != null) {
            gui.softReset();
            this.scenario = scenario;

            //enable selection menu
            //do initial drawing
            data = new DataStorer(basePath, dataset, scenario);
            solver = new Solver(data);
            data.setSolver(solver);
            //dataStorer.loadData();
            solver.setMessenger(messenger);
            gui.displayCostSurface();

            // Load solutions.
            initializeSolutionSelection(solutionChoice);
        }
    }

    public void toggleSourceDisplay(boolean show) {
        if (show) {
            /*Set<Integer> test = data.getJunctions();
            Integer[] locations = test.toArray(new Integer[0]);
            for (int temp: locations) {
                double[] rawXYLocation = data.cellLocationToRawXY(temp);
                Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
                c.setStroke(Color.BLACK);
                c.setFill(Color.BLACK);
                sinkLocationsLayer.getChildren().add(c);
            }*/

            for (Source source : data.getSources()) {
                double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 / gui.getScale());
                c.setStroke(Color.SALMON);
                c.setFill(Color.SALMON);
                //c.setStroke(Color.RED);
                //c.setFill(Color.RED);
                sourceLocationsLayer.getChildren().add(c);
            }
        } else {
            sourceLocationsLayer.getChildren().clear();
            sourceLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSourceLabels(boolean show) {
        if (show) {
            for (Source source : data.getSources()) {
                Label l = new Label(source.getLabel());
                double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                l.setTranslateX(rawXtoDisplayX(rawXYLocation[0]) + 1);
                l.setTranslateY(rawXtoDisplayX(rawXYLocation[1]) + 1);
                sourceLabelsLayer.getChildren().add(l);
            }
        } else {
            sourceLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSinkDisplay(boolean show) {
        if (show) {
            //Set<Edge> test = dataStorer.getGraphEdgeRoutes().keySet();
            //for (Edge e : test) {
            //    double[] rawXYLocation = dataStorer.cellLocationToRawXY(e.v1);
            //    Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
            //    c.setStroke(Color.ORANGE);
            //    c.setFill(Color.ORANGE);
            //    sinkLocationsLayer.getChildren().add(c);
            //    
            //    rawXYLocation = dataStorer.cellLocationToRawXY(e.v2);
            //    Circle c2 = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 1);
            //    c2.setStroke(Color.ORANGE);
            //    c2.setFill(Color.ORANGE);
            //    sinkLocationsLayer.getChildren().add(c2);
            //}

            for (Sink sink : data.getSinks()) {
                double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 / gui.getScale());
                c.setStroke(Color.CORNFLOWERBLUE);
                c.setFill(Color.CORNFLOWERBLUE);
                //c.setStroke(Color.BLUE);
                //c.setFill(Color.BLUE);
                sinkLocationsLayer.getChildren().add(c);
            }
        } else {
            sinkLocationsLayer.getChildren().clear();
            sinkLabelsLayer.getChildren().clear();
        }
    }

    public void toggleSinkLabels(boolean show) {
        if (show) {
            for (Sink sink : data.getSinks()) {
                Label l = new Label(sink.getLabel());
                double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                l.setTranslateX(rawXtoDisplayX(rawXYLocation[0]) + 1);
                l.setTranslateY(rawXtoDisplayX(rawXYLocation[1]) + 1);
                sinkLabelsLayer.getChildren().add(l);
            }
        } else {
            sinkLabelsLayer.getChildren().clear();
        }
    }

    public void generateShortestPathsNetwork() {
        if (scenario != "") {
            data.generateShortestPaths();
        }
    }

    public void generateCandidateNetwork() {
        if (scenario != "") {
            data.generateCandidateGraph();
        }
    }

    public void generateCandidateGraph() {
        if (scenario != "") {
            data.generateCandidateGraph();
        }
    }

    public void generateMPSFile(String crf, String numYears, String capacityTarget, int modelVersion) {
        if (scenario != "") {
            System.out.println("Writing MPS File...");
            data.setTargetCaptureAmount(Double.parseDouble(capacityTarget));    //Heuristic
            data.setCrf(Double.parseDouble(crf));   //Heuristic
            data.setProjectLength(Integer.parseInt(numYears));    // Heuristic
            MPSWriter.writeMPS("mip.mps", data, Double.parseDouble(crf), Double.parseDouble(numYears), Double.parseDouble(capacityTarget), basePath, dataset, scenario, modelVersion);
        }
    }

    // Price simulation
    public void runPriceSimulation(String crf, String numYears, String inputPrice, String numPairs, int modelVersion) {
        // Load simulation parmeters.
        double prices[] = DataInOut.loadPrices();
        if (prices == null) {
            prices = new double[] {Double.parseDouble(inputPrice)};
        }
        
        // Save original injection costs.
        Sink[] sinks = data.getSinks();
        double[] originalInjectionCosts = new double[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            originalInjectionCosts[i] = sinks[i].getInjectionCost();
        }
        
        // Create aggregation file.
        StringBuilder aggregateResults = new StringBuilder("");
        aggregateResults.append("CO2 Price,Captured Amount,# Opened Sources,# Opened Sinks,Network Length,");
        aggregateResults.append("Total Cost ($M/yr),Total Capture Cost ($M/yr),Total Transport Cost ($M/yr),Total Storage Cost ($M/yr),");
        aggregateResults.append("Total Unit Cost($/tCO2),Unit Capture Cost($/tCO2),Unit Transport Cost($/tCO2),Unit Storage Cost($/tCO2)\n");
        
        for (double price : prices) {
            // Set new injection costs.
            for (int i = 0; i < sinks.length; i++) {
                sinks[i].setInjectionCost(originalInjectionCosts[i] - price);
            }
            
            // Run solver.
            File solutionPriceDirectory = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + "price-" + price + "h");
            solutionPriceDirectory.mkdir();
            runHeuristic(crf, numYears, inputPrice, numPairs, modelVersion, solutionPriceDirectory);
            
            // Create shapefiles.
            Solution soln = DataInOut.loadHeuristicSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + "price-" + price + "h");
            DataInOut.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + "price-" + price + "h", soln);
            DataInOut.makeCandidateShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario);
            DataInOut.makeSolutionFile(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + "price-" + price + "h", soln);
            
            // Update aggregation file.
            aggregateResults.append(price + "," + soln.getAnnualCaptureAmount() + "," + soln.getNumOpenedSources() + "," + soln.getNumOpenedSinks() + ",TBD,");
            aggregateResults.append(soln.getTotalCost() + "," + soln.getTotalAnnualCaptureCost() + "," + soln.getTotalAnnualTransportCost() + "," + soln.getTotalAnnualStorageCost() + ",");
            aggregateResults.append(soln.getUnitTotalCost() + "," + soln.getUnitCaptureCost() + "," + soln.getUnitTransportCost() + "," + soln.getUnitStorageCost() + "\n");
        }
        
        // Write aggregation file.
        DataInOut.makePriceAggregationFile(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/aggregateResults.csv", aggregateResults.toString());
    }
    
    // Heuristic
    public void heuristicSolve(String crf, String numYears, String capacityTarget, String numPairs, int modelVersion) {
        DateFormat dateFormat = new SimpleDateFormat("ddMMyyy-HHmmssss");
        Date date = new Date();
        String run = "run" + dateFormat.format(date) + "h";
        File solutionDirectory = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + run);
        solutionDirectory.mkdir();
        
        runHeuristic(crf, numYears, capacityTarget, numPairs, modelVersion, solutionDirectory);
    }
    
    private void runHeuristic(String crf, String numYears, String capacityTarget, String numPairs, int modelVersion, File directory) {
        // Get model data
        data.setTargetCaptureAmount(Double.parseDouble(capacityTarget));
        data.setCrf(Double.parseDouble(crf));
        data.setProjectLength(Integer.parseInt(numYears));
        
        // Run heuristic
        Heuristic heuristic = new Heuristic(data);
        heuristic.solve(Integer.parseInt(numPairs), modelVersion);
        
        // Save solution
        DataInOut.saveHeuristicSolution(directory, heuristic);
    }

    public void runCPLEX() {
        // Check if CPLEX exists.
        try {
            Runtime r = Runtime.getRuntime();
            r.exec("cplex");

            // Copy mps file and make command files.
            DateFormat dateFormat = new SimpleDateFormat("ddMMyyy-HHmmssss");
            Date date = new Date();
            String run = "run" + dateFormat.format(date);
            File solutionDirectory = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + run);
            String os = System.getProperty("os.name");
            try {
                solutionDirectory.mkdir();

                // Copy MPS file into results file.
                String mipPath = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/mip.mps";
                Path from = Paths.get(mipPath);
                Path to = Paths.get(solutionDirectory + "/mip.mps");
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);

                // Make OS script file and cplex commands file.
                if (os.toLowerCase().contains("mac")) {
                    PrintWriter cplexCommands = new PrintWriter(solutionDirectory + "/cplexCommands.txt");
                    cplexCommands.println("set logfile *");
                    cplexCommands.println("read " + solutionDirectory.getAbsolutePath() + "/mip.mps");
                    cplexCommands.println("opt");
                    cplexCommands.println("write " + solutionDirectory.getAbsolutePath() + "/solution.sol");
                    cplexCommands.println("quit");
                    cplexCommands.close();

                    File osCommandsFile = new File(solutionDirectory + "/osCommands.sh");
                    PrintWriter osCommands = new PrintWriter(osCommandsFile);
                    osCommands.println("#!/bin/sh");
                    osCommands.println("cplex < " + solutionDirectory.getAbsolutePath() + "/cplexCommands.txt");
                    osCommands.close();
                    osCommandsFile.setExecutable(true);
                } else if (os.toLowerCase().contains("windows")) {
                    PrintWriter cplexCommands = new PrintWriter(solutionDirectory + "/cplexCommands.txt");
                    cplexCommands.println("read mip.mps");
                    cplexCommands.println("opt");
                    cplexCommands.println("write solution.sol");
                    cplexCommands.println("quit");
                    cplexCommands.close();

                    File osCommandsFile = new File(solutionDirectory + "/osCommands.bat");
                    PrintWriter osCommands = new PrintWriter(osCommandsFile);
                    osCommands.println("@echo off");
                    osCommands.println("cplex < cplexCommands.txt");
                    osCommands.close();
                    osCommandsFile.setExecutable(true);
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }

            try {
                if (os.toLowerCase().contains("mac")) {
                    String[] args = new String[]{"/usr/bin/open", "-a", "Terminal", solutionDirectory.getAbsolutePath() + "/osCommands.sh"};
                    ProcessBuilder pb = new ProcessBuilder(args);
                    pb.directory(solutionDirectory);
                    Process p = pb.start();
                } else if (os.toLowerCase().contains("windows")) {
                    String[] args = new String[]{"cmd.exe", "/C", "start", solutionDirectory.getAbsolutePath() + "/osCommands.bat"};
                    ProcessBuilder pb = new ProcessBuilder(args);
                    pb.directory(solutionDirectory);
                    Process p = pb.start();
                }
            } catch (IOException e) {
            }
        } catch (IOException e) {
            messenger.setText("Error: Make sure CPLEX is installed and in System PATH.");
        }
    }

    // Code to manage science gateway interface
    public void runGateway() {
        // Need to find way to tunnel through firewalls.
        if (scenario != "") {
            Stage stage = new Stage();
            StackPane root = new StackPane();
            WebView view = new WebView();
            view.getEngine().locationProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observableValue, String oldLoc, String newLoc) {
                    if (newLoc.contains("download")) {
                        DataInOut.downloadFile(newLoc);
                    }
                }
            });

            WebEngine engine = view.getEngine();
            engine.load("https://geosurveyiu.scigap.org/login");
            root.getChildren().add(view);

            Scene scene = new Scene(root, 650, 500);
            stage.setScene(scene);
            stage.show();
        }
    }

    public void toggleShortestPathsDisplay(boolean show) {
        if (show && data != null) {
            int[][] rawPaths = data.getShortestPathEdges();
            HashSet<Edge> edges = new HashSet<>();
            for (int p = 0; p < rawPaths.length; p++) {
                int[] path = rawPaths[p];
                for (int i = 0; i < path.length - 1; i++) {
                    Edge edge = new Edge(path[i], path[i + 1]);
                    edges.add(edge);
                }
            }
            for (Edge edge : edges) {
                double[] rawSrc = data.cellLocationToRawXY(edge.v1);
                double[] rawDest = data.cellLocationToRawXY(edge.v2);
                double sX = rawXtoDisplayX(rawSrc[0]);
                double sY = rawYtoDisplayY(rawSrc[1]);
                double dX = rawXtoDisplayX(rawDest[0]);
                double dY = rawYtoDisplayY(rawDest[1]);
                Line line = new Line(sX, sY, dX, dY);
                line.setStroke(Color.BLACK);
                line.setStrokeWidth(1.0 / gui.getScale());
                line.setStrokeLineCap(StrokeLineCap.ROUND);
                shortestPathsLayer.getChildren().add(line);
            }
        } else {
            shortestPathsLayer.getChildren().clear();
        }

        // Alternate way that may be useful.
        /*if (show && data != null) {
            int[][] rawPaths = data.getShortestPathEdges();
            for (int p = 0; p < rawPaths.length; p++) {
                int[] path = rawPaths[p];
                Path pathObj = new Path();
                double[] rawSrc = data.cellLocationToRawXY(path[0]);
                pathObj.getElements().add(new MoveTo(rawXtoDisplayX(rawSrc[0]), rawYtoDisplayY(rawSrc[1])));
                for (int dest = 1; dest < path.length; dest++) {
                    double[] rawDest = data.cellLocationToRawXY(path[dest]);
                    LineTo line = new LineTo(rawXtoDisplayX(rawDest[0]), rawYtoDisplayY(rawDest[1]));
                    pathObj.getElements().add(line);
                }
                shortestPathsLayer.getChildren().add(pathObj);
            }
        } else {
            shortestPathsLayer.getChildren().clear();
        }*/
    }

    public void toggleRawDelaunayDisplay(boolean show) {
        if (show & data != null) {
            HashSet<int[]> delaunayEdges = data.getDelaunayEdges();
            for (int[] path : delaunayEdges) {
                for (int src = 0; src < path.length - 1; src++) {
                    int dest = src + 1;
                    double[] rawSrc = data.cellLocationToRawXY(path[src]);
                    double[] rawDest = data.cellLocationToRawXY(path[dest]);
                    double sX = rawXtoDisplayX(rawSrc[0]);
                    double sY = rawYtoDisplayY(rawSrc[1]);
                    double dX = rawXtoDisplayX(rawDest[0]);
                    double dY = rawYtoDisplayY(rawDest[1]);
                    Line edge = new Line(sX, sY, dX, dY);
                    edge.setStroke(Color.BROWN);
                    edge.setStrokeWidth(1.0 / gui.getScale());
                    edge.setStrokeLineCap(StrokeLineCap.ROUND);
                    rawDelaunayLayer.getChildren().add(edge);
                }
            }
        } else {
            rawDelaunayLayer.getChildren().clear();
        }
    }

    public void toggleCandidateNetworkDisplay(boolean show) {
        if (show) {
            HashSet<int[]> selectedRoutes = data.getGraphEdges();
            for (int[] route : selectedRoutes) {
                for (int src = 0; src < route.length - 1; src++) {
                    int dest = src + 1;
                    double[] rawSrc = data.cellLocationToRawXY(route[src]);
                    double[] rawDest = data.cellLocationToRawXY(route[dest]);
                    double sX = rawXtoDisplayX(rawSrc[0]);
                    double sY = rawYtoDisplayY(rawSrc[1]);
                    double dX = rawXtoDisplayX(rawDest[0]);
                    double dY = rawYtoDisplayY(rawDest[1]);
                    Line edge = new Line(sX, sY, dX, dY);
                    edge.setStroke(Color.PURPLE);
                    edge.setStrokeWidth(3.0 / gui.getScale());
                    edge.setStrokeLineCap(StrokeLineCap.ROUND);
                    candidateNetworkLayer.getChildren().add(edge);
                }
            }
        } else {
            candidateNetworkLayer.getChildren().clear();
        }
    }

    public void generateMPSFiles(String crf, String numYears, String capacityTarget, int modelVersion) {
        Random r = new Random();

        Source[] sources = data.getSources();
        Sink[] sinks = data.getSinks();
        double[] sinkCapacity = new double[sinks.length];
        double[] wellCapacity = new double[sinks.length];
        for (int j = 0; j < sinks.length; j++) {
            sinkCapacity[j] = sinks[j].getCapacity();
            wellCapacity[j] = sinks[j].getWellCapacity();
        }
        for (int i = 0; i < 100; i++) {
            System.out.println("Writing MPS File " + i + "...");

            for (int j = 0; j < sinks.length; j++) {
                sinks[j].setCapacity((sinkCapacity[j] / 3.0) * r.nextGaussian() + sinkCapacity[j]);
                sinks[j].setWellCapacity((wellCapacity[j] / 3.0) * r.nextGaussian() + wellCapacity[j]);
            }
            // source production rate.
            // capture cost.
            // well capacity.
            // sink capacity.
            // injection cost.
            // STDDEV = 1/4 mean
            data.setTargetCaptureAmount(Double.parseDouble(capacityTarget));    //Heuristic
            data.setCrf(Double.parseDouble(crf));   //Heuristic
            data.setProjectLength(Integer.parseInt(numYears));    // Heuristic
            MPSWriter.writeMPS("mip" + i + ".mps", data, Double.parseDouble(crf), Double.parseDouble(numYears), Double.parseDouble(capacityTarget), basePath, dataset, scenario, modelVersion);
        }
        for (int j = 0; j < sinks.length; j++) {
            sinks[j].setCapacity(sinkCapacity[j]);
            sinks[j].setWellCapacity(wellCapacity[j]);
        }
    }

    public void initializeSolutionSelection(ChoiceBox solutionChoice) {
        if (basePath != "" && dataset != "" && scenario != "") {
            // Set initial datasets.
            File f = new File(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results");
            ArrayList<String> solns = new ArrayList<>();
            solns.add("None");
            for (File file : f.listFiles()) {
                if (file.getName().endsWith("Agg")) {
                    solns.add(file.getName());
                } else if (file.isDirectory() && file.getName().charAt(0) != '.') {
                    boolean sol = false;
                    boolean mps = false;
                    for (File subFile : file.listFiles()) {
                        if (subFile.getName().endsWith(".sol")) {
                            sol = true;
                        } else if (subFile.getName().endsWith(".mps")) {
                            mps = true;
                        }
                        // Heuristic
                        if (subFile.getName().endsWith("solution.txt")) {
                            sol = true;
                            mps = true;
                        }
                    }
                    if (sol && mps) {
                        solns.add(file.getName());
                    }
                }
            }
            solutionChoice.setItems(FXCollections.observableArrayList(solns));
        }
    }

    public void selectSolution(String file, Label[] solutionValues) {
        solutionLayer.getChildren().clear();
        for (Label l : solutionValues) {
            l.setText("-");
        }

        if (file != null && !file.equals("None")) {
            if (file.endsWith("Agg")) {
                aggregateSolutions(file, solutionValues);
            } else {
                // Heuristic
                Solution soln = null;
                if (file.endsWith("h")) {
                    soln = DataInOut.loadHeuristicSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file);
                } else {
                    soln = DataInOut.loadSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file);
                }
                HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();

                for (Edge e : soln.getOpenedEdges()) {
                    int[] route = graphEdgeRoutes.get(e);
                    for (int src = 0; src < route.length - 1; src++) {
                        int dest = src + 1;
                        double[] rawSrc = data.cellLocationToRawXY(route[src]);
                        double[] rawDest = data.cellLocationToRawXY(route[dest]);
                        double sX = rawXtoDisplayX(rawSrc[0]);
                        double sY = rawYtoDisplayY(rawSrc[1]);
                        double dX = rawXtoDisplayX(rawDest[0]);
                        double dY = rawYtoDisplayY(rawDest[1]);
                        Line edge = new Line(sX, sY, dX, dY);
                        edge.setStroke(Color.GREEN);
                        edge.setStrokeWidth(5.0 / gui.getScale());
                        edge.setStrokeLineCap(StrokeLineCap.ROUND);
                        solutionLayer.getChildren().add(edge);
                    }
                }

                for (Source source : soln.getOpenedSources()) {
                    double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
                    Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 20 / gui.getScale());
                    c.setStrokeWidth(0);
                    c.setStroke(Color.SALMON);
                    c.setFill(Color.SALMON);
                    solutionLayer.getChildren().add(c);

                    // Pie chart nodes.
                    Arc arc = new Arc();
                    arc.setCenterX(rawXtoDisplayX(rawXYLocation[0]));
                    arc.setCenterY(rawYtoDisplayY(rawXYLocation[1]));
                    arc.setRadiusX(20 / gui.getScale());
                    arc.setRadiusY(20 / gui.getScale());
                    arc.setStartAngle(0);
                    arc.setLength(soln.getPercentCaptured(source) * 360);
                    arc.setStrokeWidth(0);
                    arc.setType(ArcType.ROUND);
                    arc.setStroke(Color.RED);
                    arc.setFill(Color.RED);
                    solutionLayer.getChildren().add(arc);
                }

                for (Sink sink : soln.getOpenedSinks()) {
                    double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
                    Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 20 / gui.getScale());
                    c.setStrokeWidth(0);
                    c.setStroke(Color.CORNFLOWERBLUE);
                    c.setFill(Color.CORNFLOWERBLUE);
                    solutionLayer.getChildren().add(c);

                    // Pie chart nodes.
                    Arc arc = new Arc();
                    arc.setCenterX(rawXtoDisplayX(rawXYLocation[0]));
                    arc.setCenterY(rawYtoDisplayY(rawXYLocation[1]));
                    arc.setRadiusX(20 / gui.getScale());
                    arc.setRadiusY(20 / gui.getScale());
                    arc.setStartAngle(0);
                    arc.setLength(soln.getPercentStored(sink) * 360);
                    arc.setStrokeWidth(0);
                    arc.setType(ArcType.ROUND);
                    arc.setStroke(Color.BLUE);
                    arc.setFill(Color.BLUE);
                    solutionLayer.getChildren().add(arc);
                }

                // Update solution values.
                solutionValues[0].setText(Integer.toString(soln.getNumOpenedSources()));
                solutionValues[1].setText(Integer.toString(soln.getNumOpenedSinks()));
                solutionValues[2].setText(Double.toString(round(soln.getCaptureAmount(), 2)));
                solutionValues[3].setText(Integer.toString(soln.getNumEdgesOpened()));
                solutionValues[4].setText(Integer.toString(soln.getProjectLength()));
                solutionValues[5].setText(Double.toString(round(soln.getTotalAnnualCaptureCost(), 2)));
                solutionValues[6].setText(Double.toString(round(soln.getUnitCaptureCost(), 2)));
                solutionValues[7].setText(Double.toString(round(soln.getTotalAnnualTransportCost(), 2)));
                solutionValues[8].setText(Double.toString(round(soln.getUnitTransportCost(), 2)));
                solutionValues[9].setText(Double.toString(round(soln.getTotalAnnualStorageCost(), 2)));
                solutionValues[10].setText(Double.toString(round(soln.getUnitStorageCost(), 2)));
                solutionValues[11].setText(Double.toString(round(soln.getTotalCost(), 2)));
                solutionValues[12].setText(Double.toString(round(soln.getUnitTotalCost(), 2)));

                // Write to shapefiles.
                DataInOut.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file, soln);
                DataInOut.makeCandidateShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario);
                DataInOut.makeSolutionFile(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file, soln);
                
                //determineROW(soln, basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file);
            }
        }
    }

    public void determineROW(Solution soln, String path) {
        // read in right of way file
        String rowFileLocation = basePath + "/" + dataset + "/BaseData/CostSurface/Ascii/rows.asc";
        boolean[] rightOfWay = new boolean[data.getWidth() * data.getHeight() + 1];
        try (BufferedReader br = new BufferedReader(new FileReader(rowFileLocation))) {
            for (int i = 0; i < 6; i++) {
                br.readLine();
            }
            String line = br.readLine();
            int cellNum = 1;
            while (line != null) {
                String[] costs = line.split("\\s+");
                for (String cost : costs) {
                    int val = Integer.parseInt(cost);
                    if (val != -9999) {
                        rightOfWay[cellNum] = true;
                    }
                    cellNum++;
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // compare solution to right of way
        HashSet<Integer> usedCells = new HashSet<>();
        HashSet<Integer> rowedCells = new HashSet<>();
        HashSet<int[]> rowedPairs = new HashSet<>();
        HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();
        ArrayList<ArrayList<Integer>> existingRowRoutes = new ArrayList<>();
        ArrayList<ArrayList<Integer>> newRowRoutes = new ArrayList<>();
        for (Edge e : soln.getOpenedEdges()) {
            int[] route = graphEdgeRoutes.get(e);
            boolean existingROW = false;
            ArrayList<Integer> newRoute = new ArrayList<>();
            for (int i = 0; i < route.length - 1; i++) {
                if (rightOfWay[route[i]] && rightOfWay[route[i + 1]]) {
                    rowedPairs.add(new int[]{route[i], route[i + 1]});
                }
            }
            for (int cell : route) {
                usedCells.add(cell);
                if (rightOfWay[cell]) {
                    if (!existingROW) {
                        // swap: new ROW -> existing ROW
                        newRowRoutes.add(newRoute);
                        newRoute = new ArrayList<>();
                        existingROW = true;
                    }
                    newRoute.add(cell);
                    rowedCells.add(cell);
                } else {
                    if (existingROW) {
                        // swap: existing ROW -> new ROW
                        existingRowRoutes.add(newRoute);
                        newRoute = new ArrayList<>();
                        existingROW = false;
                    }
                    newRoute.add(cell);
                }
            }
            if (existingROW) {
                existingRowRoutes.add(newRoute);
            } else {
                newRowRoutes.add(newRoute);
            }
        }

        // display ROWed edges
        for (int[] pair : rowedPairs) {
            double[] rawSrc = data.cellLocationToRawXY(pair[0]);
            double[] rawDest = data.cellLocationToRawXY(pair[1]);
            double sX = rawXtoDisplayX(rawSrc[0]);
            double sY = rawYtoDisplayY(rawSrc[1]);
            double dX = rawXtoDisplayX(rawDest[0]);
            double dY = rawYtoDisplayY(rawDest[1]);
            Line edge = new Line(sX, sY, dX, dY);
            edge.setStroke(Color.PURPLE);
            edge.setStrokeWidth(5.0 / gui.getScale());
            edge.setStrokeLineCap(StrokeLineCap.ROUND);
            solutionLayer.getChildren().add(edge);
        }

        double percentUsed = rowedCells.size() / (double) usedCells.size();
        messenger.setText("Percent on existing ROW: " + percentUsed);
        makeRowShapeFiles("ExistingROW", path + "/shapeFiles/", existingRowRoutes);
        makeRowShapeFiles("NewROW", path + "/shapeFiles/", newRowRoutes);
    }

    public void makeRowShapeFiles(String name, String path, ArrayList<ArrayList<Integer>> routes) {
        EsriPolylineList edgeList = new EsriPolylineList();
        String[] edgeAttributeNames = {"Id", "CapID", "CapValue", "Flow", "Cost", "LengKM", "LengROW", "LengCONS", "Variable"};
        int[] edgeAttributeDecimals = {0, 0, 0, 6, 0, 0, 0, 0, 0};
        DbfTableModel edgeAttributeTable = new DbfTableModel(edgeAttributeNames.length);   //12
        for (int colNum = 0; colNum < edgeAttributeNames.length; colNum++) {
            edgeAttributeTable.setColumnName(colNum, edgeAttributeNames[colNum]);
            edgeAttributeTable.setDecimalCount(colNum, (byte) edgeAttributeDecimals[colNum]);
            edgeAttributeTable.setLength(colNum, 10);
            if (edgeAttributeNames[colNum].equals("Id")) {
                edgeAttributeTable.setType(colNum, DbfTableModel.TYPE_CHARACTER);
            } else {
                edgeAttributeTable.setType(colNum, DbfTableModel.TYPE_NUMERIC);
            }
        }

        for (ArrayList<Integer> route : routes) {
            double[] routeLatLon = new double[route.size() * 2];    // Route cells translated into: lat, lon, lat, lon,...
            for (int i = 0; i < route.size(); i++) {
                int cell = route.get(i);
                routeLatLon[i * 2] = data.cellToLatLon(cell)[0];
                routeLatLon[i * 2 + 1] = data.cellToLatLon(cell)[1];
            }

            EsriPolyline edge = new EsriPolyline(routeLatLon, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_STRAIGHT);
            edgeList.add(edge);

            // Add attributes.
            ArrayList row = new ArrayList();
            for (int i = 0; i < 3; i++) {
                row.add(0);
            }
            row.add(0);
            for (int i = 0; i < 5; i++) {
                row.add(0);
            }

            edgeAttributeTable.addRecord(row);
        }

        EsriShapeExport writeEdgeShapefiles = new EsriShapeExport(edgeList, edgeAttributeTable, path + "/" + name);
        writeEdgeShapefiles.export();
    }

    public void aggregateSolutions(String file, Label[] solutionValues) {
        Solution aggSoln = new Solution();
        HashMap<Source, Integer> sourcePopularity = new HashMap<>();
        HashMap<Sink, Integer> sinkPopularity = new HashMap<>();
        HashMap<Edge, Integer> edgePopularity = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            Solution soln = DataInOut.loadSolution(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file + "/run" + i);

            HashMap<Edge, Double> edgeTransportAmounts = soln.getEdgeTransportAmounts();
            HashMap<Source, Double> sourceCaptureAmounts = soln.getSourceCaptureAmounts();
            HashMap<Sink, Double> sinkStorageAmounts = soln.getSinkStorageAmounts();

            for (Edge e : soln.getOpenedEdges()) {
                if (!edgePopularity.containsKey(e)) {
                    edgePopularity.put(e, 1);
                } else {
                    edgePopularity.put(e, edgePopularity.get(e) + 1);
                }

                aggSoln.addEdgeTransportAmount(e, edgeTransportAmounts.get(e));
            }

            for (Source source : soln.getOpenedSources()) {
                if (!sourcePopularity.containsKey(source)) {
                    sourcePopularity.put(source, 1);
                } else {
                    sourcePopularity.put(source, sourcePopularity.get(source) + 1);
                }

                aggSoln.addSourceCaptureAmount(source, sourceCaptureAmounts.get(source));
            }

            for (Sink sink : soln.getOpenedSinks()) {
                if (!sinkPopularity.containsKey(sink)) {
                    sinkPopularity.put(sink, 1);
                } else {
                    sinkPopularity.put(sink, sinkPopularity.get(sink) + 1);
                }

                aggSoln.addSinkStorageAmount(sink, sinkStorageAmounts.get(sink));
            }
        }

        HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();
        for (Edge e : edgePopularity.keySet()) {
            int[] route = graphEdgeRoutes.get(e);
            for (int src = 0; src < route.length - 1; src++) {
                int dest = src + 1;
                double[] rawSrc = data.cellLocationToRawXY(route[src]);
                double[] rawDest = data.cellLocationToRawXY(route[dest]);
                double sX = rawXtoDisplayX(rawSrc[0]);
                double sY = rawYtoDisplayY(rawSrc[1]);
                double dX = rawXtoDisplayX(rawDest[0]);
                double dY = rawYtoDisplayY(rawDest[1]);
                Line edge = new Line(sX, sY, dX, dY);
                edge.setStroke(Color.GREEN);
                edge.setStrokeWidth(Math.ceil(edgePopularity.get(e) / 10.0) / gui.getScale());
                edge.setStrokeLineCap(StrokeLineCap.ROUND);
                solutionLayer.getChildren().add(edge);
            }
        }

        for (Source source : sourcePopularity.keySet()) {
            double[] rawXYLocation = data.cellLocationToRawXY(source.getCellNum());
            Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 * Math.ceil(sourcePopularity.get(source) / 35.0) / gui.getScale());
            c.setStroke(Color.RED);
            c.setFill(Color.RED);
            solutionLayer.getChildren().add(c);
        }

        for (Sink sink : sinkPopularity.keySet()) {
            double[] rawXYLocation = data.cellLocationToRawXY(sink.getCellNum());
            Circle c = new Circle(rawXtoDisplayX(rawXYLocation[0]), rawYtoDisplayY(rawXYLocation[1]), 5 * Math.ceil(sinkPopularity.get(sink) / 35.0) / gui.getScale());
            c.setStroke(Color.BLUE);
            c.setFill(Color.BLUE);
            solutionLayer.getChildren().add(c);
        }

        // Write to shapefiles.
        DataInOut.makeShapeFiles(basePath + "/" + dataset + "/Scenarios/" + scenario + "/Results/" + file, aggSoln);
    }

    public double rawXtoDisplayX(double rawX) {
        double widthRatio = map.getBoundsInParent().getWidth() / data.getWidth();
        // Need to offset to middle of pixel.
        return (rawX - .5) * widthRatio;
    }

    public double rawYtoDisplayY(double rawY) {
        double heightRatio = map.getBoundsInParent().getHeight() / data.getHeight();
        // Need to offset to middle of pixel.
        return (rawY - .5) * heightRatio;
    }

    public int displayXYToVectorized(double x, double y) {
        int rawX = (int) (x / (map.getBoundsInParent().getWidth() / data.getWidth())) + 1;
        int rawY = (int) (y / (map.getBoundsInParent().getHeight() / data.getHeight())) + 1;
        return data.xyToVectorized(rawX, rawY);
    }

    public double[] latLonToDisplayXY(double lat, double lon) {
        double[] rawXY = data.latLonToXY(lat, lon);
        double heightRatio = map.getBoundsInParent().getHeight() / data.getHeight();
        double widthRatio = map.getBoundsInParent().getWidth() / data.getWidth();
        return new double[]{rawXY[0] * widthRatio, rawXY[1] * heightRatio};
    }

    public void addSourceLocationsLayer(Pane layer) {
        sourceLocationsLayer = layer;
    }

    public void addSinkLocationsLayer(Pane layer) {
        sinkLocationsLayer = layer;
    }

    public void addSourceLabelsLayer(Pane layer) {
        sourceLabelsLayer = layer;
    }

    public void addSinkLabelsLayer(Pane layer) {
        sinkLabelsLayer = layer;
    }

    public void addShortestPathsLayer(Pane layer) {
        shortestPathsLayer = layer;
    }

    public void addCandidateNetworkLayer(Pane layer) {
        candidateNetworkLayer = layer;
    }

    public void addSolutionLayer(Pane layer) {
        solutionLayer = layer;
    }

    public void addRawDelaunayLayer(Pane layer) {
        rawDelaunayLayer = layer;
    }

    public DataStorer getDataStorer() {
        return data;
    }

    public void addMessenger(TextArea messenger) {
        this.messenger = messenger;
    }

    public TextArea getMessenger() {
        return messenger;
    }

    public DataStorer getData() {
        return data;
    }
}
