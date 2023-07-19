package com.example;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.json.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  public static void main(String[] args) {
    try {
      String startNode = args[0];
      String endNode = args[1];
      getApiRequest(startNode, endNode);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void getApiRequest(String startNode, String endNode) throws IOException {
    URL getUrl = new URL(
        "https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");

    HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
    connection.setRequestMethod("GET");

    int responseCode = connection.getResponseCode();

    if (responseCode == HttpURLConnection.HTTP_OK) {
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuffer jsonResponseData = new StringBuffer();
      String readLine = null;

      while ((readLine = in.readLine()) != null) {
        jsonResponseData.append(readLine);
      }

      in.close();

      String responseData = jsonResponseData.toString();
      JSONObject obj = new JSONObject(responseData);

      String instanceString = obj.getString("bpmn20Xml");

      InputStream inputStream = new ByteArrayInputStream(instanceString.getBytes("UTF-8"));
      BpmnModelInstance modelInstance = Bpmn
          .readModelFromStream(inputStream);

      BpmnModelElementInstance startElement = modelInstance.getModelElementById(startNode);
      BpmnModelElementInstance endElement = modelInstance.getModelElementById(endNode);

      if (startElement instanceof FlowNode && endElement instanceof FlowNode) {
        FlowNode start = (FlowNode) startElement;
        FlowNode end = (FlowNode) endElement;

        List<List<FlowNode>> paths = new ArrayList<>();
        List<FlowNode> currentPath = new ArrayList<>();

        findPaths(start, end, currentPath, paths);
        System.out.println("The path from " + startNode + " to " + endNode + " is:");
        for (List<FlowNode> availablePath : paths) {
          List<String> flowStrings = availablePath.stream().map(flow -> flow.getId()).collect(Collectors.toList());
          System.out.println(flowStrings.toString());
        }

      } else {
        System.out.println("Something went wrong on path from " + startNode + " to " + endNode + ".");
      }

    } else {
      System.out.println(responseCode);
    }
  }

  private static void findPaths(FlowNode currentNode, FlowNode endEvent, List<FlowNode> currentPath,
      List<List<FlowNode>> paths) {

    if (currentPath.contains(currentNode))
      return;

    currentPath.add(currentNode);

    if (currentNode == endEvent) {
      paths.add(new ArrayList<>(currentPath));
    }

    else {
      for (SequenceFlow outgoingFlow : currentNode.getOutgoing()) {
        findPaths((FlowNode) outgoingFlow.getTarget(), endEvent, currentPath, paths);
      }
    }

    currentPath.remove(currentPath.size() - 1);
  }
}
