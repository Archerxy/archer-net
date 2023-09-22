package com.archer.net;


class ChannelContextContainer {

	private Context root;
	private Object lock = new Object();
	
	public ChannelContextContainer() {}
	
	
	public ChannelContext init(Channel ch, HandlerList handlerList) {
		if(handlerList == null || handlerList.handlerCount() <= 0) {
			return null;
		}
		int index = 0;
		ChannelContext head = new ChannelContext(handlerList.at(index++), ch);
		ChannelContext cur, last = head;
		for(; index < handlerList.handlerCount(); index++) {
			cur = new ChannelContext(handlerList.at(index), ch);
			cur.last(last);
			last = cur;
		}
		save(head);
		return head;
	}
	
	public void save(ChannelContext chCtx) {
		synchronized(lock) {
			Context ctx = new Context();
			ctx.chCtx = chCtx;
			ctx.cmp = chCtx.channel().hashCode();
			if(root == null) {
				root = ctx;
			} else {
				searchAndSave(ctx);
			}
		}
	}
	
	public ChannelContext findChannelContext(Channel ch) {
		synchronized(lock) {
			if(root == null) {
				return null;
			} else {
				Context ctx = searchAndFind(ch.hashCode());
				if(ctx == null) {
					return null;
				}
				return ctx.chCtx;
			}
		}
	}
	
	public void remove(Channel ch) {
		synchronized(lock) {
			searchAndRemove(ch.hashCode());
		}
	}
	
	private Context searchAndFind(int cmp) {
		Context cur = root;
		while(true) {
			if(cur == null) {
				return null;
			}
			if(cur.cmp == cmp) {
				return cur;
			}
			if(cur.cmp > cmp) {
				cur = cur.l;
			} else {
				cur = cur.r;
			}
		}
	}
	
	private void searchAndSave(Context ctx) {
		Context cur = root;
		while(true) {
			if(cur.cmp == ctx.cmp) {
				return ;
			}
			if(cur.cmp > ctx.cmp) {
				if(cur.l == null) {
					cur.l = ctx;
					return ;
				}
				cur = cur.l;
			} else {
				if(cur.r == null) {
					cur.r = ctx;
					return ;
				}
				cur = cur.r;
			}
		}
	}
	
	private void searchAndRemove(long cmp) {
		Context cur = root, last = null;
		boolean isleft = true;
		while(true) {
			if(cur == null) {
				return ;
			}
			if(cur.cmp == cmp) {
				if(cur.l != null) {
					Context next = cur.l;
					while(next.r != null) {
						next = next.r;
					}
					next.r = cur.r;
					
					if(last == null) {
						root = cur.l;
					} else {
						if(isleft) {
							last.l = cur.l;
						} else {
							last.r = cur.l;
						}
					}
				} else if(cur.r != null) {
					if(last == null) {
						root = cur.r;
					} else {
						if(isleft) {
							last.l = cur.r;
						} else {
							last.r = cur.r;
						}
					}
				} else {
					if(last == null) {
						root = null;
					} else {
						if(isleft) {
							last.l = null;
						} else {
							last.r = null;
						}
					}
				}
				return ;
			}
			if(cur.cmp > cmp) {
				last = cur;
				cur = cur.l;
				isleft = true;
			} else {
				last =cur;
				cur = cur.r;
				isleft = false;
			}
		}
	}
	
	private class Context {
		ChannelContext chCtx;
		int cmp;
		Context l;
		Context r;
	}
}
