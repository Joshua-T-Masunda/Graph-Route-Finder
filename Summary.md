# navigator package

## CustomList.java
- replaces the ArrayList
- because we don't know how many results can be presented, we have a dynamic array
- the array is never filled because we do have an initial capacity of 16 which could never be reached and we only have 8 neighbours.
- grow() is doubling strategy.
- copy() uses a shallow copy as not to create duplicates which saves memory.

## CustomMinHeap.java
- Dihkstra and A* uses this for the cheapest cost for traversal.
- we have the heap order propertry parent <= children
- dual min/max heap
- min is for traversal
- offerTopK() switches to max heap which is for KNN

## GraphEdge.java
- this is the link between GraphNodes 
- Immutable nodes and mutable edge information

## GraphNode
- this is just the patch that holds
  image data(HSV, variance)
  classification results.
  pathfinding state
- RGB constructor is to convert RGB to HSV for the real image processing.
- HSV constructor for the demo
- setClassifiction() this is where the results of KNN are stored
- gCost is the actual cost
- hCost is the estimated cost
- getFCost() is gCost + hCost

## GraphGrid.java
- this is the graph ADT that is the actual image

## GraphSimilarity.java
- label
- obstacle density
- edge count
- average weight

# algorithm package

## HeuristicType.java
- estimate the cost to the destination
- MANHATTAN (dx + dy) is the only for cardinal movements.
- OCTILE() is ideally for 8 directional movements
- EUCLIDEAN is for linear movements

## ALL
- validate input
- reset state; so the nodes visisted, grid
- create open set(min heap) which is nodes to be visited and closed set(double[][]) which is nodes visited aloong if they are walkable
- insert our start node
- loop; extract path, check goal
- compute path
- corner cutting makes sure like the nodes next to them are walkable before moving forward
- reconstruct works backwards, from goal to start

## Dijkstra.java
- uses gCost so visits the most number of nodes so makes it really slow but accurate

## AStarSearch.java
- uses fCost = gCost + hCost so it reduces the number of nodes visited
- calculateOctileDistance gives the hCost

## GreedyBestFirstSearch.java
- uses hCost so it's really fast by visiting as little nodes as possible

# classifier package
Image -> ImageClassifier -> RichFeatureExtractor -> KNNClassifier -> GraphNode.setClassification() -> GraphSmoother

## DatasetPreprocessor.java
- scans images using DatasetLoader and creates a persistent data file
- we don't have the 20% rule for testing

## GraphSmoother.java
- cancels noises
- creates an original of the nodes
- makes the votes
- decide if to make changes

## ImageClassifier.java
- takes a buffered image and turns it into a GraphGrid

## KNNClassifier.java
- not really KNN but evoloved rather using Weka's Random Forest tree which is a 100 decision making trees with KNN as a fall back
- setupWekaStructure() defines the weka dataset schema; tells weka how many features to expect using ArrayList because their API
  doesn't except anything else.
- Instrance is an empty object that acts as a template that DenseInstance has to reference to everytime
- loadFromCSV() reads the persistent data
- buildWekaModel() 
  1. false if there is no training data
  2. if USE_WEKA is false use kNN as fall back
  3. If weka and data is available use RandomForest
-"We started with a pure hand-written k-NN classifier, which is reflected in the Neighbour class and the k parameter. As we worked on the project, we realised that Random Forest would give significantly better accuracy on our multi-class terrain problem because it handles high-dimensional feature vectors better and doesn't require distance metric tuning. We integrated Weka for the classification step — which is permitted under the spec since Weka is used for visualisation-adjacent functionality, not for the core graph construction or feature extraction. Those remain entirely hand-written. We kept the hand-written k-NN as a fallback, so the system still works without Weka. The DatasetPreprocessor and GraphSmoother reflect earlier iterations of the pipeline that were partially superseded but kept for compatibility."

# histogram package
Converts raw image pixels into feature vectors that classification can understand

## HSVConvertor.java
- converts RGB colours to HSV
- RGB mixes a lot of colours up like red and bright red, things like colour, intensity, brightness
- HSV separete
  H(Hue) actual color 0deg - 360deg
  S(Saturation) purity 0=grayscale and 1=fully vivid color
  V(Value) brightness 0=black and 1=brightest

## HistogramExtractor.java
- changes file patches into color distributions
- histogram counts how frequent something would be
- TOTAL_BINS; 12 hue occurancies + 3 s occurancies + 3 v occurancies
- extractCombinedFeatures() looks at texture

## TextureExtractor.java
- extracts variance and edge density which are texture descriptors.
- texture describes roughness, smoothness
- variance measures how intense the spread of color is
- high variance = high texture
- low variance = smooth areas
- edge density measures quick changes in intensity

## RichFeatureExtractor.java
- creates a 25-dimensional feature vector
- Index	Meaning
  0-17	HSV histogram
  18	  Variance
  19	  Edge density
  20	  Mean Hue
  21	  Mean Saturation
  22	  Mean Value
  23	  Neighbor mean Hue
  24	  Neighbor mean Saturation

## FeatureVector.java
- contains extracted features and it's data

# controller package

## AlgorithmsManager.java
- facade design pattern, sits between UI and three algorithm path finding algorithms.

# data package

## DemogridGenerator.java
- creates a synthetic map for demonstration purposes.
1. build ray nodes with RGB colour
2. extract the rich vectors
3. classify each not
4. apply spatial smoothing

## ImageLoeader.java
- takes a real image and produces an fully classifed GraphGrid
- first constructor UI belongs to the class
- second constructor allows to share a classifier

# user_interface package

## Main.java

## MapCanvas.java
- dark background
- real photo if loaded
- terrain colour
- algorithm paths
- start/end markers
- training cell
- grid lines

## PathfindingVisualizerWindow.java
- uses animation systems

### private static final double MIN_TRAVERSAL_WEIGHT = 0.8;
- A* guarentees the optimal path, so the hueristic must never over estimate the true cost

### What random number generator does
- computers cannot really generate random numbers, it can produces numbers that seem random
- computer needs a starting number called the seed and same seed always produces the same sequence
- new Random(2026) means the demo maps starts the same everytime

The Full Answer
What the system genuinely does well:
Be honest but balanced. Start by acknowledging what works — the graph-based approach correctly models spatial relationships, the weighted pathfinding correctly prefers roads over vegetation, the active learning loop allows improvement with user input, and the spatial smoother reduces classification noise. This isn't a broken system — it's a limited one.
Limitation 1 — Training data quality and bias (your image quality point, expanded):
The bootstrap colour heuristics assume daytime aerial photography with consistent lighting. South African informal settlements specifically have corrugated iron roofs that reflect sunlight unpredictably, red soil that can look similar to brick structures, and dense vegetation that shadows paths. A classifier trained on clean stock imagery will misclassify heavily in these conditions. A production system would need training data specifically sourced from South African aerial imagery — ideally labelled by people familiar with the terrain.
Limitation 2 — Temporal staleness (your second point):
Correct. A static image captures one moment. Potholes appear and are repaired. Floods make paths impassable. Informal settlements grow rapidly — a path that existed six months ago may now be blocked by a new structure. A production system would integrate with live data sources — municipal road condition APIs, satellite imagery updated on a rolling basis, or crowdsourced hazard reporting similar to Waze.
Limitation 3 — The weight table is manually tuned fiction:
Your traversal weights — ROAD=0.8, VEGETATION=15.0 — were chosen by feel, not measured from real data. In reality, the cost of traversing terrain depends on the specific use case. A pedestrian, a delivery vehicle, and an ambulance have completely different traversal costs for the same terrain. A pothole that stops a car is irrelevant to a pedestrian. A production system would have use-case-specific weight profiles derived from actual movement data.
Limitation 4 — Block-level granularity loses detail:
Your system divides images into rectangular blocks and assigns one label per block. A block might be half road and half vegetation — it gets one label and one weight. Real hazards like potholes can be smaller than one block and get averaged away entirely. A production system would need sub-block analysis or adaptive block sizing based on detected feature boundaries.
Limitation 5 — Graph similarity is structurally shallow:
Your similarity metric compares aggregate statistics — label proportions, average weight. Two completely different road networks with the same proportion of road vs vegetation would score identically. A production system would need structural graph comparison — are the road networks connected the same way? Do they have similar junction patterns? This requires graph isomorphism techniques which are computationally expensive but much more meaningful.
What a production-ready version would require:
A labelled dataset of South African aerial imagery. A proper train/validation/test split with reported accuracy metrics. Dynamic graph updates when conditions change. Use-case-specific weight profiles. Integration with existing mapping APIs. Sub-block resolution for small hazards. And critically — human validation of routes before they're recommended to real users, because a misclassified WATER node as PATH could direct someone into a flood.

The answer to give a marker:

"Genuinely hazard-aware in controlled conditions, but with significant gaps for real-world deployment. Our bootstrap heuristics assume clean daytime aerial photography which doesn't hold for informal settlement imagery where iron roofs, red soil and shadow create classification noise. Our weight table is manually tuned rather than measured — a pedestrian and a vehicle have completely different traversal costs for the same terrain. Our block-level granularity can average away small hazards like potholes. Our similarity metric compares aggregate statistics rather than structural topology so two completely different road networks can score identically. And our training data has no validation split so we can't report a formal accuracy number. A production system would need South African specific training data, dynamic updates for temporal changes as you mentioned, use-case specific weight profiles, sub-block resolution, and human validation before routes reach real users. What we've built is a strong proof of concept that demonstrates the graph-based approach is viable — but the gap between proof of concept and production is significant and we're honest about that."


Overall Performance Summary
QuestionPerformanceQ1 — Duplicate smoothersPartial — missed the "why it happened"Q2 — Dijkstra vs A*Partial — missed the core technical differenceQ3 — Heuristic factor 0.8Honest — didn't knowQ4 — decreaseKey inefficiencyPartial — wrong data structure suggestedQ5 — Weka defenceGood instinct, weak argumentQ6 — directionBitSurface level onlyQ7 — Average degreeHonest — didn't knowQ8 — Seeded randomBackwards — confused what seed doesQ9 — Runtime vs persistentPartial — missed the consequencesQ10 — Real-world limitationsGood start, needed more depth
The pattern: You understand the system at a high level but struggle when pushed to reason through specific implementation details. Tonight, focus on Q3, Q6, Q7 and Q8 — those are the ones where the gap is clearest and they're all very teachable. You now have the correct answers for all of them. Read each one out loud a few times in your own words until it feels natural.

### Presenting
Person 1 — Problem Statement & System Overview
Opens the presentation. Sets the scene.
Covers:

The South African context — why hazard-aware navigation matters here specifically. Informal settlements, potholes, flood-prone paths. Make it real and local.
The core idea — we represent an image as a graph where nodes are terrain blocks and edges are traversal connections
High-level architecture slide — show how the packages connect: data → histogram → classifier → navigator → algorithm → UI
The assignment requirements and how you meet each one — graph ADT at core, two graph-based tasks (pathfinding + similarity), custom data structures, Java desktop app

This person sets up everything. They should be your most confident speaker because first impressions matter.
Ends by handing over: "I'll now hand over to [Person 2] who will walk you through how we construct the graph from an image."

Person 2 — Graph Construction & Classification
The technical meat of the system.
Covers:

How ImageLoader takes a real image and divides it into blocks — show the block size concept visually
How GraphNode and GraphGrid represent the graph ADT — nodes, edges, the implicit edge design and why you chose it over storing edge objects
How RichFeatureExtractor turns a pixel block into a feature vector — briefly, not deep
How KNNClassifier classifies each node — mention the Weka Random Forest primary path and the handwritten k-NN fallback
How applySpatialSmoothing cleans the result — show the before/after visually if possible
The weight table — how classification results directly feed into pathfinding by setting node weights and walkability

This is the densest section. This person should be comfortable with technical language and able to handle deep questions.
Ends by handing over: "Now that the graph is built and classified, [Person 3] will show how we use it for pathfinding."

Person 3 — Pathfinding & Similarity
The algorithms section. This is your most visually impressive part.
Covers:

The three algorithms — what each prioritises, the key difference between them
The octile heuristic — why it's the right choice for 8-connected grids
The admissibility argument — why you multiply by 0.8
AlgorithmManager — how it resets state between runs and compares results
Live demonstration of pathfinding on the loaded map — show the visit order animation for all three algorithms, point out visually how Dijkstra floods while A* focuses
GraphSimilarity — how you compare two graphs and what the score means
The limitations you're aware of — average degree being near-constant, structural shallowness

Ends by handing over: "[Person 4] will now demonstrate the full system live and show the interactive visualizer."

Person 4 — Live Demo & Closing
Handles the keyboard during the full demonstration and closes the presentation.
Covers:

Load a real image — show the progress bar, talk through what's happening behind the scenes while it loads
Show the classified terrain overlay — point out roads, vegetation, structures
Place start and end points, run Find Route — show all three paths simultaneously, read the statistics panel
Toggle paths on and off to show the difference between algorithms
Open the PathfindingVisualizerWindow — draw some walls, run each algorithm, show the animated exploration
Run a comparison between two images — show the similarity score
If time allows, show training mode — click a node, assign a label, show the classifier update
Close with a summary slide — what you built, what you learned, what you'd improve

This person must be completely comfortable with the UI. Practise the demo flow until it's smooth. Have a backup plan if image loading is slow — the Demo button always works.


  → resize for processing
  → divide into blocks
  → average RGB per block
  → create GraphNode with HSV conversion
  → build full GraphGrid
  → bootstrap colour heuristics → runtime training data
  → extract 25D neighbour features per node
  → classify with Random Forest (or k-NN fallback)
  → setClassification → weight + walkable updated
  → two-pass spatial smoothing with road protection
  → GraphGrid ready for pathfinding


  Person 2 Deep Dive — Graph Construction & Classification
Your section is the most technically dense and the most important — everything else in the system depends on what you build. Let me go through exactly what you need to be able to say, in the order you'd say it.

Opening — Your First Words
When Person 1 hands over to you, don't just launch into technical detail immediately. Orient the markers first with one sentence:

"Thank you. So at this point the user has loaded an image — my job is to explain how we turn that raw image into a graph that the pathfinding algorithms can actually navigate."

That one sentence tells the marker exactly what your section achieves before you say anything technical. Never make them guess what you're about to explain.

Part 1 — Why a Graph?
Before explaining how you build the graph, explain why a graph is the right structure. Markers award marks for abstraction — showing you translated the problem domain into the correct construct.
Be ready to say:

"An image is naturally a 2D spatial structure where every region has a relationship with its neighbours. A graph models this perfectly — nodes represent terrain blocks, edges represent the ability to move between them, and edge weights represent the cost of that movement. This lets us apply standard graph algorithms like A* directly to image navigation, which wouldn't be possible with a plain pixel array."

Then point to the spec requirement — graph ADT at the core — and confirm you meet it.

Part 2 — ImageLoader — Building the Graph
The Block Size Decision
Explain that the image is divided into a grid of rectangular blocks. Each block becomes one node. The block size is configurable — smaller blocks mean more detail but more nodes and slower processing.
Be ready to explain the automatic resizing:

"We cap the image at 2200 pixels on its longest side. Beyond that, the grid becomes too large to classify and render responsively. We also have a cell count cap of 120,000 nodes — if the block size is too small for the image dimensions, we increase it automatically in steps of 2 until we're under the limit. This was a deliberate engineering decision to keep the system usable on typical hardware."

averageBlockRGB
For each block you compute the mean RGB across all pixels in that block. Know why:

"Taking the average colour of a block rather than a single pixel makes the node's colour representation more robust — a single pixel could be noise, an artifact, or a shadow. The average captures the dominant colour of that terrain region. We use long for the running sums rather than int to prevent integer overflow — a 32×32 block has 1024 pixels each up to 255, so the maximum sum per channel is 261,120 which fits in an int but we use long as defensive practice."

The Three-Phase Pipeline
Be clear that building the graph happens in three distinct phases:
Phase 1 — Node construction. Every block becomes a GraphNode with its average RGB, immediately converted to HSV.
Phase 2 — Bootstrap learning via autoLearnFromImage. Before classification can run, the classifier needs training data. You generate it automatically using colour heuristics.
Phase 3 — Classification. Every node gets classified using its 25-dimensional feature vector.
Make sure you can explain why these must be three separate phases:

"They have to be separate because Phase 2 needs all nodes to exist before it can extract neighbour features — extractWithNeighbors reads surrounding cells, so if neighbours don't exist yet the feature vectors would be incomplete. And Phase 3 needs the classifier to be trained before it can classify anything. The order is not arbitrary."


Part 3 — GraphNode — The Vertex
This is your fundamental unit. Know every field and why it exists.
RGB to HSV Conversion
This happens immediately in the constructor:
javafloat[] hsv = HSVConverter.rgbToHsv(r, g, b);
this.h = hsv[0] / 360.0;
this.s = hsv[1];
this.v = hsv[2];
Be ready to explain why HSV instead of RGB:

"RGB mixes colour and brightness together — a dark red and a bright red have very different RGB values even though they're the same colour. HSV separates hue — the actual colour — from saturation and value — the intensity. This makes colour-based terrain classification more robust to lighting variation. A road in shadow and a road in sunlight have very different RGB values but similar hue and saturation profiles."

The Dual Purpose of setClassification
This is the critical bridge between classification and pathfinding. When the classifier assigns a label, setClassification immediately updates both weight and walkable:
javathis.weight = getWeightByLabel(this.terrainLabel);
this.walkable = (this.weight < Double.POSITIVE_INFINITY);
Be ready to articulate why this design matters:

"Classification and navigation are directly coupled through this method. The moment a node is labelled WATER, it becomes impassable — weight is set to infinity and walkable becomes false. The moment it's labelled ROAD, it gets weight 0.8 — the lowest cost in the system. This means the pathfinding algorithms don't need to know anything about terrain labels — they just read weight and walkable. The classifier speaks to the navigator through these two fields."

The Weight Table
Know this by heart and be able to justify each value:
LabelWeightReasonROAD0.8Paved, fastest surfacePATH1.0Baseline traversal costPOTHOLE5.0Passable but damaging and slowVEGETATION15.0Off-road, very slowSTRUCTURE∞Building — impassableWATER∞Flood/mud — impassableUNKNOWN10.0Conservative — treat as rough terrain
Be ready to admit:

"These weights are manually tuned based on domain reasoning rather than measured from real movement data. In a production system they'd be derived empirically and would vary by use case — a pedestrian and a vehicle have completely different costs for the same terrain."

A* State Fields
gCost, hCost, parent live directly on the node. Know why:

"Storing search state directly on nodes avoids creating separate data structures to track costs and parents during pathfinding. The tradeoff is that nodes carry fields they don't use outside of search — but for a grid graph where every node participates in search anyway, this is acceptable. We reset these fields via grid.resetAll() before every search so they don't carry over between algorithm runs."


Part 4 — GraphGrid — The Graph ADT
This is your most important class to understand deeply. A marker will almost certainly ask you about it.
Why a Grid Graph
Be ready to justify the design choice:

"We chose a grid graph because images are inherently 2D spatial structures. A grid graph preserves that spatial correspondence — position in the graph directly maps to position in the image. Every node has at most 8 neighbours, which is the 8-connectivity model standard in image processing. An alternative would be a general adjacency-list graph, but that would lose the spatial structure and require explicit storage for thousands of redundant edge objects."

Implicit Edges — The Key Design Decision
This is something markers specifically look for:

"Edges in our graph are not stored as objects — they're computed on demand by getNeighbors. For a 100×100 grid that's potentially 80,000 edges. Storing them all as objects would cost significant memory and require constant synchronisation with node state. Computing them on demand costs a few arithmetic operations per call and automatically reflects the current walkability state of each node. This is an adjacency-list inspired approach adapted for grid geometry."

The removedEdgeMasks Bitmask
Be able to explain this clearly — it's sophisticated and markers notice:

"When an edge is removed, we don't delete an object — there isn't one. Instead we record that specific direction as blocked in a bitmask. Each node gets one integer in removedEdgeMasks. Each of the 8 directions maps to one bit via directionBit. Marking an edge removed is a bitwise OR, checking if it's removed is a bitwise AND, restoring it is AND with the complement. This is O(1) and uses minimal memory — one integer per node rather than a list of removed edges."

Then walk through directionBit if they ask — you now know this cold from Question 6.
8-Directional Movement and Corner Cutting
Be ready to explain both:

"We support 8-directional movement — cardinal and diagonal — because real navigation doesn't restrict you to horizontal and vertical movement. Diagonal movement costs √2 rather than 1.0, reflecting the actual Euclidean distance. We prevent corner cutting — moving diagonally through the corner of a wall — by checking that both side nodes are walkable before allowing a diagonal step. Without this, paths could clip through the corners of buildings geometrically incorrectly."

getNeighbors vs getNeighborsForFeatures
Know the distinction:

"We have two neighbour methods for different purposes. getNeighbors is for pathfinding — it respects walkability and edge removal. getNeighborsForFeatures is for classification and smoothing — it returns all 8 surrounding nodes regardless of walkability, because even an impassable building is spatially adjacent and should influence the terrain classification of surrounding blocks."


Part 5 — Feature Extraction
You don't have the histogram/ files in your section but you need to know enough to speak to feature extraction confidently since it feeds directly into your classification.
Be ready to say:

"Each node's raw colour values aren't sufficient for reliable terrain classification — two different terrain types can have similar average colours depending on lighting. Instead we extract a rich feature vector that captures multiple aspects of the patch. This includes HSV histogram bins capturing colour distribution, texture features measuring local variation, edge density measuring how sharp the boundaries are within the block, and critically — neighbour context. The extractWithNeighbors method produces a 25-dimensional vector that includes features from surrounding nodes, so the classifier learns not just what a patch looks like but what its neighbours look like. A road surrounded by other roads has a different neighbourhood signature than a road surrounded by vegetation."


Part 6 — KNNClassifier — Classification
The Two-Path Design
Be clear and confident about this:

"Our classifier has two paths. The primary path uses Weka's Random Forest — 100 decision trees that each vote on a terrain label, with the majority vote winning. The confidence score is the fraction of trees that agreed. If Weka is unavailable or the system property isn't set, we fall back to our hand-written k-NN implementation which finds the K nearest training samples by feature distance and takes a weighted majority vote, with closer neighbours weighted more heavily via inverse distance weighting."

Why Random Forest Over Pure k-NN
Be ready to justify:

"k-NN on high-dimensional feature vectors degrades — the curse of dimensionality means distances become less meaningful as dimensions increase. Our feature vectors are 25-dimensional, which is high enough that k-NN struggles to distinguish terrain types reliably. Random Forest handles high-dimensional data better because each tree only considers a random subset of features, which reduces correlation between trees and improves generalisation."

The Active Learning Loop
This is genuinely impressive — make sure you highlight it:

"The system improves with use through an active learning loop. When a user manually labels a node in training mode, addTrainingSample extracts that node's feature vector, saves it to persistent_training.csv, and immediately rebuilds the model including the new sample. The next time the application starts, that sample is loaded from CSV and included in training. Over time, the classifier gets better as users correct its mistakes — this is a real active learning pipeline, not just a static model."

Bootstrap vs Persistent Training
You now know this cold from Question 9:

"autoLearnFromImage uses setRuntimeTrainingData — not setTrainingData — deliberately. Bootstrap colour heuristics are image-specific approximations that shouldn't pollute the persistent model. If we saved them to CSV, every image load would add potentially wrong samples that would degrade classification on future images. Runtime data is ephemeral — it's generated fresh for each image and discarded when the next image loads. Only deliberate user labels are persisted."


Part 7 — Spatial Smoothing
Finish your section with this — it's the post-processing step that makes classification usable.

"After classification, each node has been classified independently based only on its own features. This produces noise — isolated blocks classified incorrectly because their local patch was ambiguous. Spatial smoothing applies a majority vote among the 8 neighbours of each node. We use a two-pass approach — first snapshot all current labels, then apply changes — because if we modified labels during the vote, later nodes would see already-changed neighbours and the smoothing would cascade unpredictably.


We have special rules for linear features. Roads and paths are protected — if a road node connects to at least 2 other road nodes, it's probably part of an actual road line and we keep its label even if surrounded by different terrain. Naive majority voting would destroy road continuity because roads are narrow linear features typically surrounded by non-road terrain. Potholes are similarly protected — requiring 7 of 8 neighbours to agree before smoothing them away, because they're small features that would otherwise be swallowed by surrounding road classifications."


Likely Questions Directed Specifically at You
Based on your section, these are the questions most likely to come your way:
"Why does GraphNode store A state fields like gCost and parent? Isn't that mixing concerns?"*

"It's a pragmatic decision. Separating search state into a parallel data structure would require mapping between nodes and their state on every access, adding complexity for minimal benefit. Since every node participates in search anyway, storing state directly is simpler and faster. The tradeoff is that GraphNode has more fields than a pure graph vertex would — we acknowledge it's not textbook clean architecture."

"Your grid uses integer coordinates but your canvas uses floating point pixels. How do you map between them?"

"The canvas computes a cell width and height by dividing the view dimensions by the grid dimensions. A node at grid position (x, y) maps to canvas pixel viewX + x * cellWidth for the centre. The inverse — pixel to node — subtracts the view offset and divides by cell size, then clamps to grid bounds. This mapping is recalculated every render to handle window resizing."

"How does the system handle images where classification fails completely — everything comes back UNKNOWN?"

"UNKNOWN nodes get weight 10.0 — treated as rough terrain, passable but expensive. This means pathfinding still works but prefers any classified terrain over unclassified regions. It's a conservative fallback. In practice this happens when the classifier has no training data, which is why the bootstrap step is important — it gives the model something to work with even on a fresh install."

"Why does GraphGrid.classifyAll exist separately from ImageLoader.buildHazardGrid? They seem to do similar things."

"They serve different contexts. GraphGrid.classifyAll was designed for use with a pre-loaded RGB array, used primarily by the demo generator. ImageLoader.buildHazardGrid handles the full pipeline including resizing, block averaging from a BufferedImage, bootstrap learning, and progress reporting. They overlap in responsibility — this is an area where given more time we'd unify them into a single classification pipeline with a clean interface."


Your Closing Handover
End your section cleanly:

"So to summarise what we've built — a graph where every node represents a classified terrain block, edges represent weighted traversal costs derived from that classification, and the whole structure is ready for graph algorithms to navigate. I'll hand over to [Person 3] who'll show you how A*, Dijkstra and Greedy actually find routes through this graph."


The One Thing to Practise Tonight
Stand up, close your notes, and explain the full pipeline out loud from memory:
Image file
  → resize for processing
  → divide into blocks
  → average RGB per block
  → create GraphNode with HSV conversion
  → build full GraphGrid
  → bootstrap colour heuristics → runtime training data
  → extract 25D neighbour features per node
  → classify with Random Forest (or k-NN fallback)
  → setClassification → weight + walkable updated
  → two-pass spatial smoothing with road protection
  → GraphGrid ready for pathfinding