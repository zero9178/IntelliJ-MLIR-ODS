
if true then {
    defvar i = 5;

    if true then {
        defvar i = !add(<caret>i, 1);
    }
}
