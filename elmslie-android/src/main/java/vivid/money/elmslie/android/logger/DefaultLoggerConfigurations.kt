package vivid.money.elmslie.android.logger

import vivid.money.elmslie.android.logger.strategy.Crash
import vivid.money.elmslie.android.logger.strategy.AndroidLog
import vivid.money.elmslie.core.config.ElmslieConfig
import vivid.money.elmslie.core.logger.strategy.IgnoreLog

fun ElmslieConfig.defaultProdLogger() = logger {
    fatal(Crash)
    nonfatal(IgnoreLog)
    debug(IgnoreLog)
}

fun ElmslieConfig.defaultDevLogger() = logger {
    fatal(Crash)
    nonfatal(AndroidLog)
    debug(AndroidLog)
}