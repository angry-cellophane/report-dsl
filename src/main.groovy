
enum CheckStatus {
    SUCCESS, FAILURE
}

enum CheckType {
    SETUP, TEST, TEARDOWN
}

class Check {
    String name
    CheckType type
    CheckStatus status = CheckStatus.SUCCESS
    Date timestamp = new Date()
}

class Report {
    List<Check> checks = []
    CheckStatus status = CheckStatus.SUCCESS
    Date timestamp = new Date()
}

class ReportSpec {
    Map<String, Closure<?>> tests = [:]
    Map<String, Closure<?>> teardowns = [:]
    Map<String, Closure<?>> setups = [:]
    final Report report

    ReportSpec(Report report) {
        this.report = report
    }

    void test(String name, Closure<?> checkSpec) {
        tests << [(name): checkSpec]
    }

    void setup(String name, Closure<?> setupSpec) {
        setups << [(name): setupSpec]
    }

    void teardown(String name, Closure<?> teardownSpec) {
        teardowns << [(name): teardownSpec]
    }
}

static void publish(Report report) {
    println "report status: ${report.status} ${report.timestamp}"
    report.checks.each { println "${it.timestamp} ${it.type}: ${it.name} -> ${it.status}" }
}

enum RunType {
    FAIL_FAST,
    RUN_ALL
}

static void report(@DelegatesTo(value = ReportSpec, strategy = Closure.DELEGATE_FIRST) Closure<?> setupReportSpec) {
    def report = new Report()
    def reportSpec = new ReportSpec(report)

    setupReportSpec.setDelegate(reportSpec)
    setupReportSpec.setResolveStrategy(Closure.DELEGATE_FIRST)

    try {
        setupReportSpec()

        runChecks(CheckType.SETUP, report, reportSpec.setups, RunType.FAIL_FAST)
        runChecks(CheckType.TEST, report, reportSpec.tests, RunType.FAIL_FAST)
    } catch (Throwable e) {
        e.printStackTrace()
        report.status = CheckStatus.FAILURE
    } finally {
        runChecks(CheckType.TEARDOWN, report, reportSpec.teardowns, RunType.RUN_ALL)
        publish(report)
    }
}

static void runChecks(CheckType type, Report report, Map<String, Closure<?>> checks, RunType runType) {
    checks.each { e ->
        def check = new Check(name: e.key, type: type)
        try {
            e.value()
        } catch (Throwable ex) {
            ex.printStackTrace()
            check.status = CheckStatus.FAILURE
            if (runType == RunType.FAIL_FAST) throw ex
        } finally {
            report.checks << check
        }
    }
}

report {
    setup('create db', {
        println 'DB created'
    })

    setup 'init db tables', {
        println 'DB initialized'
    }

    test '1 == 1', {
        if (1 == 1) {
            println '1 == 1'
        } else {
            throw new AssertionError()
        }
    }

    test '2 != 1', {
        if (2 == 1) {
            println '2 == 1. o_O'
        } else {
            throw new AssertionError()
        }
    }

    test 'you never see me', {
        if (true) throw new AssertionError()
    }

    teardown 'remove db', {
        println 'db remove'
    }
}

