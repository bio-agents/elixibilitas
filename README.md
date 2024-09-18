```diff
- THE PROJECT WAS MOVED TO THE https://gitlab.bsc.es/inb/iechor/agents-platform/elixibilitas/elixibilitas-rest
```
#### Elixibilitas: Quality Metrics agent for iEchor BioAgents Registry.

###### Enterprise Java 8 (JEE8) Platform
Elixibilitas project is strictly adherent to the [JEE8](https://www.jcp.org/en/jsr/detail?id=366) specification.
The agent is developed and deployed on [WildFly 14.1](http://wildfly.org/) server, 
but should run on other servers (i.e. [Apache Tomcat](http://tomcat.apache.org/)).

###### MongoDB
Quality Metrics are stored in [MongoDB](www.mongodb.com)

###### Apache Maven build system
To simplify build process Elixibilitas uses [Apache Maven](https://maven.apache.org/) build system.

Modules are logically separated by their functionality:
- openebench-agents-model - Java [JSON-B](https://www.jcp.org/en/jsr/detail?id=367) model classes for the agents.
- elixibilitas-metrics-model - Java [JSON-B](https://www.jcp.org/en/jsr/detail?id=367) model classes for quality metrics.
- elixibilitas-dao - MongoDB data access classes for "agents" and "metrics" collection management.
- elixibilitas-rest - RESTful API.

###### OWL2 Ontology
There is an ontological view to the OpenEBench agents.

The agents ontology is located at:

> [https://openebench.bsc.es/monitor/agents.owl](https://openebench.bsc.es/monitor/agents.owl)

The agents data may be obtained in OWL2 JSON-LD format for any concrete agent record:<br/>
> [https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org](https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org)

or for the entire agents collection:<br/>
> [https://openebench.bsc.es/monitor/agent/](https://openebench.bsc.es/monitor/agent/)

The decision to return JSON or JSON-LD is taken on the HTTP "Accept" header.  
<br/>
These URLs may be imported into [Protegé](https://protege.stanford.edu/) agent.  
**NB:** Entire ontology is very big and exceeds the default Protegé memory settings.


###### REST API
The REST API is based on [JAX-RS](jcp.org/en/jsr/detail?id=370) API.

> Open API 3.0 (aka Swagger): [https://openebench.bsc.es/monitor/openapi.json](https://openebench.bsc.es/monitor/openapi.json)  
> Agent JSON Schema: [https://openebench.bsc.es/monitor/agent/agent.json](https://openebench.bsc.es/monitor/agent/agent.json)  
> Metrics JSON Schema: [https://openebench.bsc.es/monitor/metrics/metrics.json](https://openebench.bsc.es/monitor/metrics/metrics.json)  
<br/>

The API provides an access to biological agents descriptions:
```
https://openebench.bsc.es/monitor/agent/
```
> Returns all OpenEBench agents.  
> The pagination is implemented via the HTTP Range Header (i.g. "Range: agents=10-30").  
> The response always contains the HTTP Content-Range Header ("Content-Range: agents 10-30/20000").  
> Alternatively, there are two query parameters: {from} and {to} that may be used instead of the Range Header:  
>
> example: [https://openebench.bsc.es/monitor/agent?from=10&to=20](https://openebench.bsc.es/monitor/agent?from=10&to=20) .  
```
https://openebench.bsc.es/monitor/agent/{id}
https://openebench.bsc.es/monitor/agent/{id}/{type}
https://openebench.bsc.es/monitor/agent/{id}/{type}/{host}
https://openebench.bsc.es/monitor/agent/{id}/{type}/{host}/{path}
```
where:
- {id} is the prefixed agent id (i.e. "bioagents:pmut")
- {type} is a type of the agent ("web", "app", "cmd", "db", "rest", "soap")
- {host} is the agent provider which is usually provider's host
- {path} is a JSON pointer to locate sub-property to return

---

> Note that {id}/{type}/{host} uniquely identify the agent, while omitting the {type} or {host} returns an array of descriptions.  <br/>
> example 1: [https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org](https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org) .<br/>
> example 2: [https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org/credits](https://openebench.bsc.es/monitor/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org/credits) .<br/>
> curl patch agent data example: 
```
curl -v -X PATCH -u user:password -H 'Content-Type: application/json' /
https://openebench.bsc.es/monitor/agent/{id}/description -d '"new description."'
```

---
It is also possible to get the list of all agents identifiers providing "Accept: text/uri-list" HTTP header:
```
curl -v -H 'Accept: text/uri-list' https://openebench.bsc.es/monitor/agent
```

Quality Metrics accessed via:
```
https://openebench.bsc.es/monitor/metrics/
https://openebench.bsc.es/monitor/metrics/{id}/{type}/{host}/{path}
```
> example 1: [https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org](https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org) .<br/>
> example 2: [https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org/project/website](https://openebench.bsc.es/monitor/metrics/bioagents:pmut:2017/web/mmb.irbbarcelona.org/project/website) .<br/>
> curl patch metrics data example: 
```
curl -v -X PATCH -u user:password -H 'Content-Type: application/json' /
https://openebench.bsc.es/monitor/metrics/{id}/support/email -d 'true'
```
or, what is the same:
```
curl -v -X PATCH -u user:password -H 'Content-Type: application/json' /
https://openebench.bsc.es/monitor/metrics/{id} -d '{"support.email": true}'
```
the former patches the json using JSON Patch, while the latter uses mongodb 'upsert' notation.

---
It is possible to query agents:
```
https://openebench.bsc.es/monitor/rest/search?id={id}&{projection}&{text}&{name}&{description}
```
where:
- {id} is the compound agent id (i.e. "pmut", "bioagents:pmut", ":pmut:2017")
- {projection} agents properties to return
- {text} text to search
> The method is thought for the client's GUI that may use a pagination mechanism.  
> The pagination is implemented via the HTTP Range Header (i.g. "Range: agents=10-30").  
> The response always contains the HTTP Content-Range Header ("Content-Range: agents 10-30/10000").  
> When pagination is used, the server seponds with 206 Partial Content.  
> There is also possibility to use query parameters {skip} and {limit} (range 10-30 = skip 10, limit 20).  

<br/>

> The results are grouped by the id and sorted by names.  <br/>
> example 1: [https://openebench.bsc.es/monitor/rest/search](https://openebench.bsc.es/monitor/rest/search) .<br/>
> example 2: [https://openebench.bsc.es/monitor/rest/search](https://openebench.bsc.es/monitor/rest/search) .<br/>
> example 3: [https://openebench.bsc.es/monitor/rest/search?id=pmut](https://openebench.bsc.es/monitor/rest/search?id=pmut) .<br/>
> example 4: [https://openebench.bsc.es/monitor/rest/search?text=alignment](https://openebench.bsc.es/monitor/rest/search?text=alignment)  .<br/>

---

The METRICS API that aggregates some metrics:
```
https://openebench.bsc.es/monitor/rest/metrics/availability/{id}
```
> example: [https://openebench.bsc.es/monitor/rest/metrics/availability/trimal](https://openebench.bsc.es/monitor/rest/metrics/availability/trimal) .<br/>

---

The API also provides EDAM descriptions for the agent:
```
https://openebench.bsc.es/monitor/rest/edam/agent/
https://openebench.bsc.es/monitor/rest/edam/agent/{id}/{type}/{host}
```
> example: [https://openebench.bsc.es/monitor/rest/edam/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org](https://openebench.bsc.es/monitor/rest/edam/agent/bioagents:pmut:2017/web/mmb.irbbarcelona.org) .

or descriptions of the EDAM term itself:
```
https://openebench.bsc.es/monitor/rest/edam/description?term={edam id}
```
> example: [https://openebench.bsc.es/monitor/rest/edam/description?term=http://edamontology.org/format_3607](https://openebench.bsc.es/monitor/rest/edam/description?term=http://edamontology.org/format_3607) .

There is also full text search over EDAM ontology.
```
https://iechor.bsc.es/monitor/rest/edam/search?text={text to search}
```
> example: [https://openebench.bsc.es/monitor/rest/edam/search?text=alignment](https://openebench.bsc.es/monitor/rest/edam/search?text=alignment) .

There are simple stat info that can be obtained from the server:

>[https://openebench.bsc.es/monitor/rest/statistics](https://openebench.bsc.es/monitor/rest/statistics) : basic statistics.  
>[https://openebench.bsc.es/monitor/rest/statistics/total](https://openebench.bsc.es/monitor/rest/statistics/total) : total number of agents.  
>[https://openebench.bsc.es/monitor/rest/statistics/operational](https://openebench.bsc.es/monitor/rest/statistics/operational) : number of agents those homepage is accessible.

```
https://openebench.bsc.es/monitor/rest/statistics/{agent_type} : number of agents of particular type ("web", "cmd", etc.)
```
> example: [https://openebench.bsc.es/monitor/rest/statistics/cmd](https://openebench.bsc.es/monitor/rest/statistics/cmd) .

All changes are stored in a log collection and could be accessed:

```
https://openebench.bsc.es/monitor/agents/log/{id}/{type}/{host}/{path}
https://openebench.bsc.es/monitor/metrics/log/{id}/{type}/{host}/{path}
```
> example: [https://openebench.bsc.es/monitor/metrics/log/bioagents:pmut:2017/cmd/mmb.irbbarcelona.org/project/website/operational](https://openebench.bsc.es/monitor/metrics/log/bioagents:pmut:2017/cmd/mmb.irbbarcelona.org/project/website/operational) .
