import Foundation
// *** const ***
let MEMOSIZE :Int = 4
let MEMOS :Int = 257
let charset141 :[Int] = [512,1,0,0,0,0,0,0]
let choice142 :[UInt8] = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
let choice144 :[UInt8] = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,2,0,0,0,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
let charset147 :[Int] = [0,67043328,0,0,0,0,0,0]
let choice148 :[UInt8] = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,2,2,2,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
let SYMBOLs :[String] = ["","right","left","AddExpr","SubExpr","ModExpr","MulExpr","DivExpr","IntExpr","error"]
let ParseError :Int = 9
let data149 :[UInt8] = []
let VALUEs :[[UInt8]] = [data149]
let LENGTHs :[Int] = [0]
// *** libs ***
class TreeLog{
  var op :Int
  var log :Int
  var tree :Any?
  var prevLog :TreeLog?
  var nextLog :TreeLog?
  init(_ op :Int,_ log :Int,_ tree :Any?,_ prevLog :TreeLog?,_ nextLog :TreeLog?) {
    self.op = op
    self.log = log
    self.tree = tree
    self.prevLog = prevLog
    self.nextLog = nextLog
  }
}
class State{
  var ntag :Int
  var cnt :Int
  var value :[UInt8]
  var prevState :State?
  init(_ ntag :Int,_ cnt :Int,_ value :[UInt8],_ prevState :State?) {
    self.ntag = ntag
    self.cnt = cnt
    self.value = value
    self.prevState = prevState
  }
}
class MemoEntry{
  var key :Int
  var result :Int
  var pos :Int
  var tree :Any?
  var state :State?
  init(_ key :Int,_ result :Int,_ pos :Int,_ tree :Any?,_ state :State?) {
    self.key = key
    self.result = result
    self.pos = pos
    self.tree = tree
    self.state = state
  }
}
class NezParserContext{
    var inputs :[UInt8]
    var length :Int
    var pos :Int
    var headpos :Int
    var tree :Any?
    var treeLog :TreeLog?
    let newFunc :(String,[UInt8],Int,Int,Int) -> Any?
    let setFunc :(Any?,Int,String,[Any?]) -> Any?
    var state :State?
    var memos :[MemoEntry]
    init(_ inputs :[UInt8],_ length :Int,_ pos :Int,_ headpos :Int,_ tree :Any?,_ treeLog :TreeLog?,_ newFunc :@escaping (String,[UInt8],Int,Int,Int) -> Any?,_ setFunc :@escaping (Any?,Int,String,[Any?]) -> Any?,_ state :State?,_ memos :[MemoEntry]) {
        self.inputs = inputs
        self.length = length
        self.pos = pos
        self.headpos = headpos
        self.tree = tree
        self.treeLog = treeLog
        self.newFunc = newFunc
        self.setFunc = setFunc
        self.state = state
        self.memos = memos
    }
}
func newMemos (_ tree :Any?,_ length :Int) -> [MemoEntry] {
  let memos: [MemoEntry] = Array(repeating:MemoEntry(-1,0,0,nil,nil), count:length)
  return memos
}
func neof (_ px :NezParserContext) -> Bool {
  return px.pos < px.length
}
func move (_ px :NezParserContext,_ shift :Int) -> Bool {
  px.pos = px.pos + shift
  return true
}
func back1 (_ px :NezParserContext,_ pos :Int) -> Bool {
  px.pos = pos
  return true
}
func many1 (_ px :NezParserContext,_ f :(NezParserContext) -> Bool) -> Bool {
  var pos: Int = px.pos
  while f(px) {
    pos = px.pos
  }
  return back1(px,pos)
}
func longkey (_ key :Int,_ memoPoint :Int) -> Int {
  return key * 64 + memoPoint
}
func getMemo (_ px :NezParserContext,_ key :Int) -> MemoEntry {
  return px.memos[key % 257]
}
func consumeM2 (_ px :NezParserContext,_ m :MemoEntry) -> Int {
  px.pos = m.pos
  px.tree = m.tree
  return m.result
}
func lookupM2 (_ px :NezParserContext,_ memoPoint :Int) -> Int {
  let key: Int = longkey(px.pos,memoPoint)
  let m: MemoEntry = getMemo(px,key)
  return (m.key == key) ? (consumeM2(px,m)) : (2)
}
func storeM (_ px :NezParserContext,_ memoPoint :Int,_ pos :Int,_ matched :Bool) -> Bool {
  let key: Int = longkey(pos,memoPoint)
  let m: MemoEntry = getMemo(px,key)
  m.key = key
  m.result = (matched) ? (1) : (0)
  m.pos = (matched) ? (px.pos) : (pos)
  m.tree = px.tree
  return matched
}
func memo2 (_ px :NezParserContext,_ memoPoint :Int,_ f :(NezParserContext) -> Bool) -> Bool {
  let pos: Int = px.pos
  switch lookupM2(px,memoPoint) {
    case 1 : return true
    case 2 : return storeM(px,memoPoint,pos,f(px))
    default : return false
  }
  return false
}
func useTreeLog (_ px :NezParserContext) -> TreeLog {
    let tcur: TreeLog = px.treeLog!
    if tcur.nextLog == nil {
        tcur.nextLog = TreeLog(0,0,nil,px.treeLog,nil)
    }
    return tcur.nextLog!
}
func logT (_ px :NezParserContext,_ op :Int,_ log :Int,_ tree :Any?) -> Bool {
    let tcur: TreeLog = useTreeLog(px)
    tcur.op = op
    tcur.log = log
    tcur.tree = tree
    px.treeLog = tcur
    return true
}
func backLink (_ px :NezParserContext,_ treeLog :TreeLog?,_ nlabel :Int,_ tree :Any?) -> Bool {
  px.treeLog = treeLog
  let a = linkT(px,nlabel)
  px.tree = tree
  return a
}
func link2 (_ px :NezParserContext,_ nlabel :Int,_ f :(NezParserContext) -> Bool) -> Bool {
  let treeLog: TreeLog? = px.treeLog
  let tree: Any? = px.tree
  return f(px) && backLink(px,treeLog,nlabel,tree)
}
func gettag (_ ntag :Int) -> String {
  return String(SYMBOLs[ntag])
}
func getlabel (_ nlabel :Int) -> String {
  return String(SYMBOLs[nlabel])
}
func getvalue (_ nvalue :Int) -> [UInt8] {
  return [UInt8](VALUEs[nvalue])
}
func getlength (_ nvalue :Int) -> Int {
  return Int(LENGTHs[nvalue])
}
func endT (_ px :NezParserContext,_ shift :Int,_ ntag0 :Int) -> Bool {
    let epos: Int = px.pos + shift
    var tcur: TreeLog? = px.treeLog
    var ntag: Int = ntag0
    var nvalue: Int = 0
    var cnt: Int = 0
    while tcur?.op != 0 {
        if tcur!.op == 3 {
            cnt = cnt + 1
        }
        else if ntag == 0 && tcur!.op == 1 {
            ntag = tcur!.log
        }
        else if nvalue == 0 && tcur!.op == 2 {
            nvalue = tcur!.log
        }
        tcur = tcur!.prevLog
    }
    if let tcur2 = tcur {
      px.tree = (nvalue == 0) ? (px.newFunc(gettag(ntag),px.inputs,tcur2.log,epos,cnt)) : (px.newFunc(gettag(ntag),getvalue(nvalue),0,getlength(nvalue),cnt))
    }else{
      px.tree = px.newFunc(gettag(ntag),getvalue(nvalue),0,getlength(nvalue),cnt)
    }
    tcur = px.treeLog
    while tcur?.op != 0 {
      if let tcur3 = tcur {
        if tcur3.op == 3 {
            cnt -= 1
            px.tree = px.setFunc(px.tree,cnt,getlabel(tcur3.log),[tcur3.tree])
        }
        tcur = tcur3.prevLog
      }
    }
    if let tcur4 = tcur {
      px.treeLog = tcur4.prevLog
    }else{
      px.treeLog = nil
    }
    return true
}

func beginT (_ px :NezParserContext,_ shift :Int) -> Bool {
  return logT(px,0,px.pos + shift,nil)
}
func linkT (_ px :NezParserContext,_ nlabel :Int) -> Bool {
  return logT(px,3,nlabel,px.tree)
}
func foldT (_ px :NezParserContext,_ shift :Int,_ nlabel :Int) -> Bool {
  return beginT(px,shift) && linkT(px,nlabel)
}
func back3 (_ px :NezParserContext,_ pos :Int,_ treeLog :TreeLog?,_ tree :Any?) -> Bool {
  px.pos = pos
  px.treeLog = treeLog
  px.tree = tree
  return true
}
func many3 (_ px :NezParserContext,_ f :(NezParserContext) -> Bool) -> Bool {
  var pos: Int = px.pos
  var treeLog: TreeLog? = px.treeLog
  var tree: Any? = px.tree
  while f(px) {
    pos = px.pos
    treeLog = px.treeLog
    tree = px.tree
  }
  return back3(px,pos,treeLog,tree)
}
func tagT (_ px :NezParserContext,_ ntag :Int) -> Bool {
  return logT(px,1,ntag,nil)
}
func nextbyte (_ px :NezParserContext) -> Int {
  let c: Int = Int(px.inputs[px.pos])
  px.pos = px.pos + 1
  return c
}
func next1 (_ px :NezParserContext,_ c :Int) -> Bool {
  return nextbyte(px) == c
}
func bits32 (_ bits :[Int],_ n :Int) -> Bool {
  return (bits[n / 32] & (1 << (n % 32))) != 0
}
func getbyte (_ px :NezParserContext) -> Int {
  return Int(px.inputs[px.pos])
}
func many9 (_ px :NezParserContext,_ f :(NezParserContext) -> Bool) -> Bool {
  var pos: Int = px.pos
  var cnt: Int = 0
  while f(px) {
    pos = px.pos
    cnt = cnt + 1
  }
  return cnt > 0 && back1(px,pos)
}
// [\t ]*
func e8 (_ px :NezParserContext) -> Bool {
  return many1(px,{(p0:NezParserContext) -> Bool in return bits32(charset141,nextbyte(p0))})
}
// '%' [\t ]*
func t143 (_ px :NezParserContext) -> Bool {
  return next1(px,37) && e8(px)
}
// <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr>
func e5 (_ px :NezParserContext) -> Bool {
  switch choice144[getbyte(px)] {
    case 1 : return t143(px) && tagT(px,5)
    case 2 : return next1(px,42) && e8(px) && tagT(px,6)
    case 3 : return next1(px,47) && e8(px) && tagT(px,7)
    default : return false
  }
  return false
}
// [0-9]+
func e10 (_ px :NezParserContext) -> Bool {
  return many9(px,{(p0:NezParserContext) -> Bool in return bits32(charset147,nextbyte(p0))})
}
// ')' [\t ]*
func t146 (_ px :NezParserContext) -> Bool {
  return next1(px,41) && e8(px)
}
// '(' [\t ]*
func t145 (_ px :NezParserContext) -> Bool {
  return next1(px,40) && e8(px)
}
// <switch '('->math:"(" math:Expression math:")" [0-9]->{[0-9]+ #IntExpr } [\t ]*>
func e9 (_ px :NezParserContext) -> Bool {
  switch choice148[getbyte(px)] {
    case 1 : return t145(px) && math_Expression(px) && t146(px)
    case 2 : return beginT(px,0) && e10(px) && endT(px,0,8) && e8(px)
    default : return false
  }
  return false
}
// <switch '('->math:"(" math:Expression math:")" [0-9]->{[0-9]+ #IntExpr } [\t ]*>
func math_Value (_ px :NezParserContext) -> Bool {
  return memo2(px,3,e9)
}
// $right(math:Value)
func e6 (_ px :NezParserContext) -> Bool {
  return link2(px,1,math_Value)
}
// {$left <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr> $right(math:Value) }*
func e7 (_ px :NezParserContext) -> Bool {
  return many3(px,{(p0:NezParserContext) -> Bool in return foldT(p0,0,2) && e5(p0) && e6(p0) && endT(p0,0,0)})
}
// math:Value {$left <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr> $right(math:Value) }*
func math_Product (_ px :NezParserContext) -> Bool {
  return memo2(px,2,{(p1:NezParserContext) -> Bool in return math_Value(p1) && e7(p1)})
}
// '+' [\t ]*
func t140 (_ px :NezParserContext) -> Bool {
  return next1(px,43) && e8(px)
}
// <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr>
func e2 (_ px :NezParserContext) -> Bool {
  switch choice142[getbyte(px)] {
    case 1 : return t140(px) && tagT(px,3)
    case 2 : return next1(px,45) && e8(px) && tagT(px,4)
    default : return false
  }
  return false
}
// $right(math:Product)
func e3 (_ px :NezParserContext) -> Bool {
  return link2(px,1,math_Product)
}
// {$left <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr> $right(math:Product) }*
func e4 (_ px :NezParserContext) -> Bool {
  return many3(px,{(p0:NezParserContext) -> Bool in return foldT(p0,0,2) && e2(p0) && e3(p0) && endT(p0,0,0)})
}
// math:Product {$left <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr> $right(math:Product) }*
func math_Expression (_ px :NezParserContext) -> Bool {
  return memo2(px,1,{(p1:NezParserContext) -> Bool in return math_Product(p1) && e4(p1)})
}
// .*
func e1 (_ px :NezParserContext) -> Bool {
  return many1(px,{(p0:NezParserContext) -> Bool in return neof(p0) && move(p0,1)})
}
// math:Expression .*
func e0 (_ px :NezParserContext) -> Bool {
  return math_Expression(px) && e1(px)
}
func parse (_ inputs :[UInt8],_ length :Int,_ newFunc :@escaping (String,[UInt8],Int,Int,Int) -> Any?,_ setFunc :@escaping (Any?,Int,String,[Any?]) -> Any?) -> Any? {
    var tree: Any? = newAST(gettag(0),inputs,0,length,0)
    let px: NezParserContext = NezParserContext(inputs,length,0,0,tree,TreeLog(0,0,tree,nil,nil),newFunc ,setFunc ,nil,newMemos(tree,257))
    tree = (e0(px)) ? (px.tree) : (newAST(gettag(ParseError),inputs,px.headpos,length,0))
    return tree
}
func parseText (_ text :String,_ newFunc :@escaping (String,[UInt8],Int,Int,Int) -> Any?,_ setFunc :@escaping (Any?,Int,String,[Any?]) -> Any?) -> Any? {
    let inputs: [UInt8] = [UInt8](text.utf8)
    let length: Int = inputs.count
    return parse(inputs,length,newFunc,setFunc)
}
class AST{
  var key :String
  var value :[Any?]
  init(_ key :String, _ value :[Any?]){
    self.key = key
    self.value = value
  }
}
let newAST = {(_ tag :String,_ inputs :[UInt8],_ spos :Int,_ epos :Int,_ n :Int) -> Any? in
    if(n == 0){
      let pre = inputs.prefix(epos-spos+1)
      return AST(tag,pre.dropFirst(spos).map{$0})
    }else{
      return AST(tag,Array(repeating:AST("",[]), count:n))
  }
}
let subAST = {(_ parent :Any?,_ n :Int,_ label :String,_ child :[Any?]) -> Any? in
  var childs = (parent as! AST).value
  if childs.count > n {
    childs[n] = AST(label, child)
  }
  (parent as! AST).value = childs
  return parent
}
func inputs(_ input:String) -> String {
  if let path = Bundle.main.path(forResource: input, ofType: "txt") {
    if let data = NSData(contentsOfFile: path){
      return String(NSString(data: data as Data, encoding: String.Encoding.utf8.rawValue)! as String)!
    }
  }
  return input
}

let standardInput = FileHandle.standardInput
while true {
  print(">> ")
  let input = standardInput.availableData
  if input.count == 1 { break }
  let start = Date()
  let str = String(describing: NSString(data:input, encoding:String.Encoding.utf8.rawValue)!)
  let inputString = str.substring(to:str.index(str.endIndex, offsetBy: -1))
  let object = parseText(inputs(inputString), newAST, subAST)
  let time = Date().timeIntervalSince(start)
  if (object != nil) {
    print("\(inputString) OK \(time)[s]: ")
  }else{
    print("\(inputString) NG \(time)[s]: ")
  }
  print(object!)
}
