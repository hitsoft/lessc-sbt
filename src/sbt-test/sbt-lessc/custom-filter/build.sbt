seq(lessSettings:_*)

(LessKeys.entryFilter in (Compile, LessKeys.less)) := ("*.less" - "*.lib.less")
