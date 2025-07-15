defvar test0 = 0;
defvar test1 = 5{0-1};
defvar test2 = 5.lol;
defvar test3 = 5{0};
defvar test4 = 5{0...1};
defvar test5 = 5[0];
defvar test6 = 5[0, 0];
defvar test7 = 5[0, 0...3];
defvar test8 = 5[0, 0-3];
defvar test9 = "text" "text";
defvar test10 = [{ }];
defvar test11 = true;
defvar test12 = false;
defvar test13 = ?;
defvar test14 = [];
defvar test15 = [0];
defvar test16 = [0, ];
defvar test17 = [0, ]<bit>;
defvar test18 = (0);
defvar test19 = (0:$var);
defvar test20 = ($var);
defvar test21 = test20;
defvar test22 = (0 $var);
defvar test23 = (0 $var, $var2);
defvar test24 = test23<>;
defvar test25 = test23<0>;
defvar test26 = test23<j=0>;
defvar test27 = test23<2, j=0>;
defvar test28 = !neq(0, 0);
defvar test29 = !cond(
    true : false,
    false : true
);
defvar test30 = "text" # 5.lol;
defvar test31 = !foreach(a, [5], a);
defvar test32 = !foldl(0, [5], acc, i, !add(acc, i));
defvar test33 = 5{test18-test0};
defvar test34 = 5{test18...test0};
