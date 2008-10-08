package org.basex.query.xquery.func;

import static org.basex.query.xquery.XQText.*;
import org.basex.BaseX;
import org.basex.query.xquery.XQException;
import org.basex.query.xquery.XQContext;
import org.basex.query.xquery.item.Item;
import org.basex.query.xquery.item.NCN;
import org.basex.query.xquery.item.Nod;
import org.basex.query.xquery.item.QNm;
import org.basex.query.xquery.item.Str;
import org.basex.query.xquery.item.Type;
import org.basex.query.xquery.item.Uri;
import org.basex.query.xquery.iter.Iter;
import org.basex.query.xquery.iter.SeqIter;
import org.basex.query.xquery.util.Err;
import org.basex.util.Token;
import org.basex.util.TokenList;
import org.basex.util.XMLToken;

/**
 * QName functions.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
final class FNQName extends Fun {
  @Override
  public Iter iter(final XQContext ctx, final Iter[] arg) throws XQException {
    switch(func) {
      case RESQNAME:
        Item it = arg[0].atomic(this, true);
        if(it == null) return Iter.EMPTY;
        QNm nm = new QNm(Token.trim(checkStr(it)), ctx);
        byte[] pre = nm.pre();
        it = arg[1].atomic(this, false);
        final Nod n = (Nod) check(it, Type.ELM);
        nm.uri = n.qname().uri;
        if(nm.uri != Uri.EMPTY) return nm.iter();

        Item i;
        final Iter iter = inscope(ctx, n);
        while((i = iter.next()) != null) {
          final byte[] ns = i.str();
          if(ns.length == 0) continue;
          if(Token.eq(pre, ns)) {
            nm.uri = ctx.ns.uri(ns);
            return nm.iter();
          }
        }

        if(pre.length != 0) Err.or(NSDECL, pre);
        nm.uri = ctx.nsElem;
        return nm.iter();
      case QNAME:
        it = arg[0].atomic(this, true);
        final Uri uri = Uri.uri(it == null ? Token.EMPTY :
          check(it, Type.STR).str());
        it = arg[1].atomic(this, true);
        it = it == null ? Str.ZERO : check(it, Type.STR);
        final byte[] str = it.str();
        if(!XMLToken.isQName(str)) Err.value(Type.QNM, it);
        nm = new QNm(str, uri);
        if(nm.ns() && uri == Uri.EMPTY) Err.value(Type.URI, uri);
        return nm.iter();
      case LOCNAMEQNAME:
        it = arg[0].atomic(this, true);
        if(it == null) return Iter.EMPTY;
        return new NCN(((QNm) check(it, Type.QNM)).ln()).iter();
      case PREQNAME:
        it = arg[0].atomic(this, true);
        if(it == null) return Iter.EMPTY;
        nm = (QNm) check(it, Type.QNM);
        return !nm.ns() ? Iter.EMPTY : new NCN(nm.pre()).iter();
      case NSURIPRE:
        it = arg[1].atomic(this, false);
        check(it, Type.ELM);
        try {
          pre = checkStr(arg[0]);
          return (pre.length == 0 ? ctx.nsElem : ctx.ns.uri(pre)).iter();
        } catch(final XQException e) {
          return Iter.EMPTY;
        }
      case INSCOPE:
        it = arg[0].atomic(this, false);
        return inscope(ctx, (Nod) check(it, Type.ELM));
      case RESURI:
        it = arg[0].atomic(this, true);
        if(it == null) return Iter.EMPTY;
        final Uri rel = Uri.uri(checkStr(it));
        if(!rel.valid()) Err.or(URIINV, it);
        
        final Uri base = arg.length == 1 ? ctx.baseURI :
          Uri.uri(checkStr(arg[1].atomic(this, false)));
        if(!base.valid()) Err.or(URIINV, base);

        return base.resolve(rel).iter();
      default:
        BaseX.notexpected(func); return null;
    }
  }

  /**
   * Returns the in-scope prefixes for the specified node.
   * @param ctx query context
   * @param node node
   * @return prefix sequence
   */
  private Iter inscope(final XQContext ctx, final Nod node) {
    final TokenList tl = new TokenList();
    tl.add(Token.XML);
    if(ctx.nsElem != Uri.EMPTY) tl.add(Token.EMPTY);

    Nod n = node;
    while(n != null) {
      final QNm[] at = n.ns();
      if(at == null) break;
      for(final QNm name : at) {
        if(name.ns()) {
          final byte[] pre = name.ln();
          if(!tl.contains(pre)) tl.add(pre);
        }
      }
      n = n.parent();
    }
    final SeqIter seq = new SeqIter();
    for(int t = 0; t < tl.size; t++) seq.add(Str.get(tl.list[t]));
    return seq;
  }
}
