-- sandbox --

_VERSION = "Lua 5.2 - Figura"

-- yeet FileIO and gc globals
-- FIXME: TESTING ONLY
debug = { getinfo = debug.getinfo }
dofile = nil
loadfile = nil
collectgarbage = nil

-- GS easter egg
_GS = _G