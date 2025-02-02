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

