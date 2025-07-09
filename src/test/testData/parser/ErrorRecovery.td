def

def Foo {
  int i = 0;
}

def Bar : Foo

def Foo {
  int i = 0;
}

class Foobar<>;

def Foo {
  int i = 0;
}

def Bar : Foo<

def Foo {
  int i = 0;
}

def Error {
  int i = ;
}

def Foo {
  int i = 0;
}

def Error {
  int i = Test<,;
}

def Foo {
  int i = 0;
}

class Foo<string s = "", Bar>;

class Foo<string s = "", > {
  int i = 0;
}


defvar value = [
def
      "GenerateOp", "PragmaOp",
class
      "IfOp", "ReduceOp", "ScanOp"
];