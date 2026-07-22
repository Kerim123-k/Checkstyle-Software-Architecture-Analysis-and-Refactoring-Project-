#!/usr/bin/env python3
"""Run the actual Cypher constraints from jqassistant/rules/pipe-and-filter.xml
   against the live jQAssistant Neo4j store (bolt://localhost:7687) and dump
   the result rows to disk so gen_report_images.py can render real screenshots."""
import json
from pathlib import Path
from neo4j import GraphDatabase

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "scripts" / "jqa-results.json"

QUERIES = {
    "Q1": """
        MATCH (filter:Class)-[:IMPLEMENTS]->(:Type {fqn:
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter"})
        WHERE NOT filter.fqn CONTAINS "Test$"
        MATCH (filter)-[:DEPENDS_ON]->(target:Type)
        WHERE NOT target.fqn STARTS WITH "java."
          AND NOT target.fqn STARTS WITH "javax."
          AND NOT target.fqn STARTS WITH
              "com.puppycrawl.tools.checkstyle.checks.pipeline."
          AND NOT target.fqn STARTS WITH
              "com.puppycrawl.tools.checkstyle.checks.metrics.pipeline."
          AND NOT target.fqn STARTS WITH
              "com.puppycrawl.tools.checkstyle.checks.sizes.pipeline."
          AND NOT target.fqn STARTS WITH
              "com.puppycrawl.tools.checkstyle.api."
          AND NOT target.fqn STARTS WITH
              "com.puppycrawl.tools.checkstyle.utils."
          AND NOT target.fqn IN
              ["void","int","boolean","byte","short","long","float","double","char"]
        RETURN filter.fqn AS Filter, target.fqn AS DisallowedDependency
    """,
    "Q2": """
        MATCH (filter:Class)-[:EXTENDS*]->(base:Type)
        WHERE (filter.fqn STARTS WITH
                "com.puppycrawl.tools.checkstyle.checks.pipeline."
            OR filter.fqn STARTS WITH
                "com.puppycrawl.tools.checkstyle.checks.metrics.pipeline."
            OR filter.fqn STARTS WITH
                "com.puppycrawl.tools.checkstyle.checks.sizes.pipeline.")
          AND base.fqn IN [
            "com.puppycrawl.tools.checkstyle.api.AbstractCheck",
            "com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck"
          ]
        RETURN filter.fqn AS OffendingFilter, base.fqn AS ForbiddenBase
    """,
    "Q3": """
        MATCH (filter:Class)-[:IMPLEMENTS]->(:Type {fqn:
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter"})
        WHERE NOT filter.fqn CONTAINS "Test$"
        MATCH (filter)-[:DECLARES]->(method:Method {name: "process"})
        MATCH (method)-[:HAS]->(p1:Parameter {index: 0})-[:OF_TYPE]->(t1:Type)
        MATCH (method)-[:HAS]->(p2:Parameter {index: 1})-[:OF_TYPE]->(t2:Type)
        RETURN filter.fqn AS Filter, t1.fqn AS In, t2.fqn AS Out
        ORDER BY filter.fqn
    """,
    "Q4": """
        MATCH (a:Class)-[:IMPLEMENTS]->(:Type {fqn:
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter"})
        MATCH (b:Class)-[:IMPLEMENTS]->(:Type {fqn:
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter"})
        WHERE a <> b
          AND NOT a.fqn CONTAINS "Test$"
          AND NOT b.fqn CONTAINS "Test$"
        MATCH path = (a)-[:DEPENDS_ON*]->(b)
        MATCH back = (b)-[:DEPENDS_ON*]->(a)
        RETURN a.fqn AS FilterA, b.fqn AS FilterB
    """,
    "Q5": """
        MATCH (filter:Class)-[:IMPLEMENTS]->(:Type {fqn:
            "com.puppycrawl.tools.checkstyle.checks.pipeline.Filter"})
        MATCH (filter)-[:DECLARES]->(:Method)-[:INVOKES]->(target:Method
            {name: "log"})
        MATCH (target)<-[:DECLARES]-(owner:Type)
        WHERE owner.fqn IN [
          "com.puppycrawl.tools.checkstyle.api.AbstractCheck",
          "com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck"
        ]
        RETURN filter.fqn AS Filter, owner.fqn AS LoggerOwner
    """,
}

results = {}
d = GraphDatabase.driver("bolt://localhost:7687", auth=None)
with d.session() as s:
    for tag, q in QUERIES.items():
        t0 = s.run("RETURN timestamp() AS t").single()["t"]
        rec = s.run(q)
        rows = [list(r.values()) for r in rec]
        cols = rec.keys()
        t1 = s.run("RETURN timestamp() AS t").single()["t"]
        results[tag] = {
            "columns": list(cols),
            "rows": rows,
            "elapsed_ms": t1 - t0,
        }
        print(f"{tag}: cols={list(cols)} rows={len(rows)} elapsed={t1 - t0}ms")
d.close()

OUT.write_text(json.dumps(results, indent=2))
print(f"wrote {OUT}")
