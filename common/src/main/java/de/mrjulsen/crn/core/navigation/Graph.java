package de.mrjulsen.crn.core.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.GlobalTrainData;
import de.mrjulsen.crn.data.Route;
import de.mrjulsen.crn.data.RoutePart;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.SimulatedTrainSchedule;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.world.level.Level;

public class Graph {

    protected static final int MIN_START_TIME = 200;

    private Map<UUID, Node> nodesById;
    private Map<TrainStationAlias, Node> nodesByStation;

    private Map<UUID, Edge> edgesById;
    private Map<Node, Map<Node, Set<Edge>>> edgesByNode;

    private Map<UUID, TrainSchedule> schedulesById;
    private Map<UUID, TrainSchedule> schedulesByTrainId;
    private Map<TrainSchedule, Set<UUID>> trainIdsBySchedule;
    private Map<UUID, UUID> scheduleIdByTrainId;

    private final GlobalSettings globalSettings;
    private final UserSettings settings;

    private final long lastUpdated;
    private final Level level;

    public Graph(Level level, UserSettings settings) {
        long startTime = System.currentTimeMillis();
        lastUpdated = level.getDayTime();
        this.settings = settings;
        this.level = level;
        GlobalTrainData.makeSnapshot(lastUpdated);

        this.nodesById = new HashMap<>();
        this.nodesByStation = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.edgesByNode = new HashMap<>();
        this.schedulesById = new HashMap<>();
        this.schedulesByTrainId = new HashMap<>();
        this.trainIdsBySchedule = new HashMap<>();
        this.scheduleIdByTrainId = new HashMap<>();

        final int[] trainCounter = new int[] { 0 };
        globalSettings = GlobalSettingsManager.getInstance().getSettingsData();
        
        TrainUtils.getAllTrains().stream().filter(x -> TrainUtils.isTrainValid(x) && !globalSettings.isTrainBlacklisted(x) && !settings.isTrainExcluded(x, globalSettings)).forEach(x -> {
            addTrain(x, globalSettings);
            trainCounter[0]++;
        });
        
        long estimatedTime = System.currentTimeMillis() - startTime;
        CreateRailwaysNavigator.LOGGER.info(String.format("Graph generated. Took %sms. Contains %s nodes, %s edges and %s schedules. %s train processed.",
            estimatedTime,
            nodesById.size(),
            edgesById.size(),
            schedulesById.size(),
            trainCounter.length
        ));
    }

    public UserSettings getSettings() {
        return settings;
    }

    protected Node addNode(TrainStationAlias alias, Train train) {
        if (nodesByStation.containsKey(alias)) {
            Node node = nodesByStation.get(alias);
            node.addTrain(train.id);
            return node;
        }

        UUID id = UUID.randomUUID();
        while (nodesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Node node = new Node(alias, id);
        node.addTrain(train.id);
        nodesById.put(id, node);
        nodesByStation.put(alias, node);
        return node;
    }

    protected Edge addEdge(Node node1, Node node2, UUID scheduleId) {
        UUID id = UUID.randomUUID();
        while (edgesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Edge edge = new Edge(node1, node2, id, scheduleId);
        if (putConnection(node1, node2, edge)) {
            edgesById.put(id, edge);
        }
        return edge;
    }

    protected TrainSchedule addTrain(Train train, GlobalSettings settingsInstance) {

        if (schedulesByTrainId.containsKey(train.id)) {
            return schedulesByTrainId.get(train.id);
        }

        UUID id = UUID.randomUUID();
        while (edgesById.containsKey(id)) {
            id = UUID.randomUUID();
        }

        TrainSchedule schedule = new TrainSchedule(train, id, settingsInstance);
        if (!schedule.addToGraph(this, train)) {
            return null;
        }

        if (trainIdsBySchedule.containsKey(schedule)) {
            TrainSchedule sched = schedulesByTrainId.get(trainIdsBySchedule.get(schedule).stream().findFirst().get());
            trainIdsBySchedule.get(schedule).add(train.id);
            schedulesByTrainId.put(train.id, sched);
            scheduleIdByTrainId.put(train.id, sched.getId());

            sched.getEdges().forEach(x -> {
                Optional<Edge> matchingEdge = schedule.getEdges().stream().filter(a -> a.equals(x)).findFirst();

                if (matchingEdge.isPresent()) {
                    x.withCost(matchingEdge.get().getCost(), false);
                }
            });

            return sched;
        }

        schedulesById.put(id, schedule);
        schedulesByTrainId.put(train.id, schedule);
        trainIdsBySchedule.put(schedule, new HashSet<>(Set.of(train.id)));
        scheduleIdByTrainId.put(train.id, schedule.getId());

        return schedule;
    }

    public boolean putConnection(Node node1, Node node2, Edge edge) {
		Map<Node, Set<Edge>> connections = edgesByNode.computeIfAbsent(node1, n -> new IdentityHashMap<>());
		if (connections.containsKey(node2)) {
            if (connections.get(node2).contains(edge)) {
                return false;
            }
            return connections.get(node2).add(edge);
        }

		return connections.put(node2, new HashSet<>(Set.of(edge))) == null;
	}

    public Set<Node> getNodes() {
        return new HashSet<>(nodesById.values());
    }

    public Node getNode(TrainStationAlias alias) {
        return nodesByStation.get(alias);
    }

    public Node getNode(UUID id) {
        return nodesById.get(id);
    }

    public Map<Node, Set<Edge>> getEdges(Node node) {
        return edgesByNode.get(node);
    }

    public Set<Edge> getEdges() {
        return new HashSet<>(edgesById.values());
    }

    public Edge getEdge(UUID id) {
        return edgesById.get(id);
    }

    public Map<Node, Set<Edge>> getConnectionsFrom(Node node) {
		if (node == null) {
			return null;
        }
		return edgesByNode.getOrDefault(node, new HashMap<>());
	}

    public Collection<Route> navigate(TrainStationAlias start, TrainStationAlias end, boolean avoidTransfers) {
        return searchTrains(searchRoute(start, end, avoidTransfers)).stream().filter(x -> !x.isEmpty()).sorted(Comparator.comparingInt(x -> x.getStartStation().getPrediction().getTicks())).toList();
    }

    public List<Node> searchRoute(TrainStationAlias start, TrainStationAlias end, boolean avoidTransfers) {
        
        if (!nodesByStation.containsKey(start) || !nodesByStation.containsKey(end)) {
            return List.of();
        }

        Map<TrainStationAlias, Node> nodes = dijkstra(start, avoidTransfers);
        Node endNode = nodes.get(end);

        if (endNode == null) {
            return List.of();
        }

        endNode.setIsTransferPoint(true);
        List<Node> route = new ArrayList<>();

        Node currentNode = endNode;
        while (!currentNode.getStationAlias().equals(start)) {
            route.add(0, currentNode);

            if (currentNode.getPreviousEdge() != null) {
                if (currentNode.getPreviousNode().getPreviousEdge() == null) {                
                    currentNode.getPreviousNode().setIsTransferPoint(false);
                } else {
                    currentNode.getPreviousNode().setIsTransferPoint(!currentNode.getPreviousEdge().getScheduleId().equals(currentNode.getPreviousNode().getPreviousEdge().getScheduleId()));
                }
            }
            currentNode = currentNode.getPreviousNode();            
        }
        currentNode.getPreviousNode().setIsTransferPoint(true);
        route.add(0, currentNode.getPreviousNode());

        return route;
    }

    private Map<UUID, SimpleTrainSchedule> generateTrainSchedules() {
        return GlobalTrainData.getInstance().getAllTrains().stream().filter(x -> 
            TrainUtils.isTrainValid(x) &&
            !globalSettings.isTrainBlacklisted(x) &&
            !settings.isTrainExcluded(x, globalSettings)
        ).collect(Collectors.toMap(x -> x.id, x -> new SimpleTrainSchedule(x)));
    }

    public Collection<Route> searchTrains(List<Node> routeNodes) {
        Map<UUID, SimpleTrainSchedule> schedulesByTrain = generateTrainSchedules();
        Collection<Route> routes = new ArrayList<>();
        routes.add(new Route(lastUpdated));

        int timer = MIN_START_TIME;
        final Node[] filteredTransferNodes = routeNodes.stream().filter(x -> x.isTransferPoint()).toArray(Node[]::new);

        if (filteredTransferNodes.length < 2) {
            return routes;
        }


        final Node lastTransfer = filteredTransferNodes[0];
        final Map<UUID, Integer> lastTransferByTrain = new HashMap<>();
        final Node lastNode = lastTransfer;
        final int simulationTime = timer;

        Collection<SimulatedTrainSchedule> trainPredictions = GlobalTrainData.getInstance().getDepartingTrainsAt(lastNode.getStationAlias()).stream()
        .filter(x -> {
            if (globalSettings.isTrainBlacklisted(x.getTrain()) || settings.isTrainExcluded(x.getTrain(), globalSettings)) {
                return false;
            }

            SimpleTrainSchedule schedule = schedulesByTrain.get(x.getTrain().id);
            for (int i = filteredTransferNodes.length - 1; i > 0; i--) {
                Node nde = filteredTransferNodes[i];
                final int j = i;
                if (nde.getTrainIds().contains(x.getTrain().id)) {
                    lastTransferByTrain.put(x.getTrain().id, j);
                    break;
                }
            }

            boolean b = lastTransferByTrain.containsKey(x.getTrain().id) &&
                        schedule.hasStationAlias(filteredTransferNodes[lastTransferByTrain.get(x.getTrain().id)].getStationAlias()) &&
                        TrainUtils.isTrainValid(x.getTrain());

            return b;
        }).map(x -> {
            int t = x.getTicks() + TrainListener.getInstance().getDepartmentTime(level, x.getTrain());
            return schedulesByTrain.get(x.getTrain().id).simulate(x.getTrain(), t > simulationTime ? 0 : simulationTime, lastNode.getStationAlias());
        }).sorted(Comparator.comparingInt(x -> x.getSimulationData().simulationCorrection())).toList();
 
        SimulatedTrainSchedule selectedPrediction = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), filteredTransferNodes[lastTransferByTrain.get(x.getSimulationData().train().id)].getStationAlias())).findFirst().orElse(trainPredictions.stream().findFirst().orElse(null));
        
        if (selectedPrediction == null) {
            CreateRailwaysNavigator.LOGGER.warn("Unable to find route from " + lastNode.getStationAlias().getAliasName());
            return routes;
        }

        Collection<SimulatedTrainSchedule> filteredSchedules = trainPredictions.stream().collect(Collectors.toMap(x -> x.getSimulationData().train().id, x -> x, (o, n) -> {
            if (n.isInDirection(lastNode.getStationAlias(), filteredTransferNodes[lastTransferByTrain.get(o.getSimulationData().train().id)].getStationAlias())) {
                return n;
            } else if (o.isInDirection(lastNode.getStationAlias(), filteredTransferNodes[lastTransferByTrain.get(o.getSimulationData().train().id)].getStationAlias())) {
                return o;
            }
            return o.getSimulationData().simulationCorrection() < n.getSimulationData().simulationCorrection() ? o : n;
        })).values();
        if (filteredSchedules.isEmpty()) {
            filteredSchedules = List.of(selectedPrediction);
        }

        for (SimulatedTrainSchedule sched : filteredSchedules) {
            Route r = new Route(lastUpdated);
            int t = sched.getFirstStopOf(lastNode.getStationAlias()).get().getPrediction().getTicks() + TrainListener.getInstance().getDepartmentTime(level, sched.getSimulationData().train());
            RoutePart part = new RoutePart(level, sched.getSimulationData().train(), lastNode.getStationAlias(), filteredTransferNodes[lastTransferByTrain.get(sched.getSimulationData().train().id)].getStationAlias(), t > simulationTime ? 0 : simulationTime);
            r.addPart(part);
            timer = part.getEndStation().getPrediction().getTicks() + settings.getTransferTime();
            Set<SimpleTrainSchedule> excludedSchedules = new HashSet<>();
            excludedSchedules.add(schedulesByTrain.get(part.getTrain().id));

            Collection<RoutePart> parts = searchTrainsInternal(schedulesByTrain, new HashSet<>(excludedSchedules), filteredTransferNodes, lastTransferByTrain.get(sched.getSimulationData().train().id) + 1, timer, filteredTransferNodes[lastTransferByTrain.get(sched.getSimulationData().train().id)]);
            parts.forEach(x -> r.addPart(x));
            routes.add(r);
        }

        return routes;
    }

    public Collection<RoutePart> searchTrainsInternal(Map<UUID, SimpleTrainSchedule> schedulesByTrain, Set<SimpleTrainSchedule> excludedSchedules, Node[] filteredTransferNodes, int startIdx, int timer, Node lastTransfer) {
        List<RoutePart> routes = new ArrayList<>();

        final int len = filteredTransferNodes.length;
        for (int i = startIdx; i < len; i++) {
            Node node = filteredTransferNodes[i];

            if (lastTransfer != null) {
                final Node lastNode = lastTransfer;
                final int simulationTime = timer;

                Collection<SimulatedTrainSchedule> trainPredictions = GlobalTrainData.getInstance().getDepartingTrainsAt(lastNode.getStationAlias()).stream()
                .filter(x -> {
                    if (globalSettings.isTrainBlacklisted(x.getTrain()) || settings.isTrainExcluded(x.getTrain(), globalSettings)) {
                        return false;
                    }
                    
                    SimpleTrainSchedule schedule = schedulesByTrain.get(x.getTrain().id);

                    boolean b = !excludedSchedules.contains(schedule) &&
                                schedule.hasStationAlias(node.getStationAlias()) &&
                                TrainUtils.isTrainValid(x.getTrain());                    
                    return b;
                }).map(x -> {
                    return schedulesByTrain.get(x.getTrain().id).simulate(x.getTrain(), simulationTime, lastNode.getStationAlias());
                }).sorted(Comparator.comparingInt(x -> x.getSimulationData().simulationCorrection())).toList();

                SimulatedTrainSchedule selectedPrediction = trainPredictions.stream().filter(x -> x.isInDirection(lastNode.getStationAlias(), node.getStationAlias())).findFirst().orElse(trainPredictions.stream().findFirst().orElse(null));
                
                if (selectedPrediction == null) {
                    CreateRailwaysNavigator.LOGGER.warn("Route aborted! No train was found at " + lastNode.getStationAlias().getAliasName());
                    return routes;
                }

                RoutePart part = new RoutePart(level, selectedPrediction.getSimulationData().train(), lastNode.getStationAlias(), node.getStationAlias(), simulationTime);
                routes.add(part);
                timer = part.getEndStation().getPrediction().getTicks() + settings.getTransferTime();
                excludedSchedules.add(schedulesByTrain.get(part.getTrain().id));
            }

            lastTransfer = node;
        }

        return routes;
    }


    protected Map<TrainStationAlias, Node> dijkstra(TrainStationAlias start, boolean avoidTransfers) {
        nodesById.values().forEach(x -> x.init());
        Node startNode = nodesByStation.get(start);
        startNode.setCost(0);
        startNode.setPrevious(startNode, null);
        startNode.setIsTransferPoint(true);

        PriorityQueue<Node> queue = new PriorityQueue<>();
        Map<TrainStationAlias, Node> excludedNodes = new HashMap<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Map<Node, Set<Edge>> reachableNodes = edgesByNode.get(currentNode);            

            reachableNodes.entrySet().stream().filter(x -> !excludedNodes.containsKey(x.getKey().getStationAlias())).forEach(y -> {
                final Node node = y.getKey();
                y.getValue().forEach(x -> {
                    Edge edge = x;
                    boolean isTransfer = currentNode.getPreviousEdge() != null && !currentNode.getPreviousEdge().getScheduleId().equals(edge.getScheduleId());

                    TrainSchedule sched = schedulesById.get(edge.getScheduleId());
                    int avgTransferTime = (int)trainIdsBySchedule.get(sched).stream().mapToInt(a -> TrainListener.getInstance().getApproximatedTrainDuration(a)).average().getAsDouble();

                    long newCost = currentNode.getCost() + edge.getCost() + (isTransfer && avoidTransfers ? avgTransferTime + 1000 : 0);
                    if (newCost > node.getCost()) {
                        return;
                    }

                    node.setCost(newCost);
                    node.setPrevious(currentNode, edge);

                    queue.add(node);
                });
            });

            excludedNodes.put(currentNode.getStationAlias(), currentNode);
        }

        return excludedNodes;
    }
}
