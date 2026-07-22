/*
 * C4 model for the Checkstyle metrics + sizes Pipe-and-Filter slice.
 * L1 (System Context) -> L2 (Containers) -> L3 (Components) -> L4 (Code).
 * Implements FR-011 / T096-T098.
 *
 * Render with:
 *   docker run -it --rm -p 8080:8080 -v "$PWD/structurizr:/usr/local/structurizr" \
 *     structurizr/lite
 */
workspace "Checkstyle Pipe-and-Filter Slice" "Architecture model for the metrics + sizes refactor." {

    model {
        developer = person "Java Developer" "Runs Checkstyle in CI or locally."
        ciSystem  = softwareSystem "CI System" "Invokes Checkstyle CLI / Maven plugin." "External"

        checkstyle = softwareSystem "Checkstyle" "Static analysis tool for Java source code." {

            cli = container "CLI / Plugin" "Entry point. Parses XML config, instantiates Checker." "Java"

            checker = container "Checker" "Loads modules, walks files, dispatches AST events." "Java"

            treeWalker = container "TreeWalker" "Builds the AST and dispatches visitToken/leaveToken to registered checks." "Java"

            slice = container "Pipe-and-Filter Slice" "16 metrics+sizes checks rebuilt as Pipeline Drivers feeding typed unidirectional pipes." "Java" {

                /* L3 components */
                driver = component "Pipeline Driver (*Check)" "Thin shim. Owns framework setters; builds the per-file Pipeline; forwards visit/leave tokens as AstEvent; drains ViolationMessages back to log()." "Java"

                tokenFilter        = component "TokenFilter"        "Forwards AstEvents whose AST type is in the configured set; passes BEGIN/FINISH_TREE through." "Filter<AstEvent,AstEvent>"
                lineSplitter       = component "LineSplitterFilter" "Splits FileText into 1-based FileLine messages." "Filter<FileText,FileLine>"
                ignorePattern      = component "IgnorePatternFilter" "Drops FileLines matching the configured regex (LineLength only)." "Filter<FileLine,FileLine>"
                measurementFilter  = component "<X>MeasurementFilter" "Per-check algorithm body, byte-for-byte from the original visitToken/leaveToken. Emits Measurement (or ViolationMessage for JavaNCSS)." "Filter<AstEvent|FileLine,Measurement>"
                couplingFilter     = component "CouplingMeasurementFilter" "Tracks PACKAGE_DEF / IMPORT context and counts referenced types per class. Used by both coupling drivers via constructor parameters." "Filter<AstEvent,Measurement>"
                thresholdFilter    = component "ThresholdFilter" "Compares Measurement.value > max; emits ViolationMessage when exceeded." "Filter<Measurement,ViolationMessage>"
                violationSink      = component "ViolationSink"  "Terminal QueuePipe drain mount; the driver pulls from here after each submit." "Filter<ViolationMessage,ViolationMessage>"

                pipeline = component "Pipeline / PipelineBuilder" "Immutable ordered chain of (filter, pipe) pairs. Validates input/output type compatibility at build time." "Java"

                singletonPipe = component "SingletonPipe<T>" "Single-message slot used on the AST hot path." "Pipe<T>"
                queuePipe     = component "QueuePipe<T>"     "FIFO buffer used on splitter (1->N) and sink (N violations per file) edges." "Pipe<T>"

                /* L3 wiring (typed pipes) */
                driver -> pipeline             "Builds once per beginTree; submits AstEvent / FileText messages"
                driver -> violationSink        "Drains ViolationMessages and forwards to AbstractCheck.log(..)"

                pipeline -> tokenFilter        "Stage 1 (AST drivers)"
                pipeline -> lineSplitter       "Stage 1 (file-level drivers)"
                lineSplitter -> ignorePattern  "FileLine pipe (LineLength only)"
                ignorePattern -> measurementFilter "FileLine pipe (LineLength only)"
                tokenFilter -> measurementFilter "AstEvent pipe"
                tokenFilter -> couplingFilter  "AstEvent pipe (coupling drivers)"
                measurementFilter -> thresholdFilter "Measurement pipe"
                couplingFilter -> thresholdFilter   "Measurement pipe"
                thresholdFilter -> violationSink     "ViolationMessage pipe"

                pipeline -> singletonPipe "Allocates between adjacent stages on AST hot path"
                pipeline -> queuePipe     "Allocates between splitter / sink stages"
            }

            /* L2 wiring */
            cli -> checker      "Configures and runs"
            checker -> treeWalker "Parses files, dispatches AST"
            treeWalker -> slice  "Calls visitToken / leaveToken on Pipeline Drivers"
        }

        developer -> cli "Runs"
        ciSystem  -> cli "Invokes"
    }

    views {
        systemContext checkstyle "L1-Context" {
            include *
            autoLayout
        }

        container checkstyle "L2-Containers" {
            include *
            autoLayout
        }

        component slice "L3-Components" {
            include *
            autoLayout
        }

        /*
         * L4 code-level view — exemplar MethodLengthCheck pipeline.
         * In Structurizr DSL the L4 code view is typically generated from
         * the source by a code parser. The component view above already
         * captures the four-filter chain that defines the pilot.
         */

        styles {
            element "External" { background #999999 color #ffffff }
            element "Filter<AstEvent,AstEvent>"           { background #f5deb3 }
            element "Filter<FileText,FileLine>"           { background #f5deb3 }
            element "Filter<FileLine,FileLine>"           { background #f5deb3 }
            element "Filter<AstEvent|FileLine,Measurement>" { background #ffe4b5 }
            element "Filter<AstEvent,Measurement>"        { background #ffe4b5 }
            element "Filter<Measurement,ViolationMessage>" { background #ffd700 }
            element "Filter<ViolationMessage,ViolationMessage>" { background #ffa500 }
            element "Pipe<T>" { background #d3d3d3 shape Pipe }
        }

        theme default
    }
}
