import React, { useState, useEffect, useCallback } from 'react'
import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL || ''

// attach JWT to every request if we have one
axios.interceptors.request.use(cfg => {
  const token = sessionStorage.getItem('nexops_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// ── design tokens ────────────────────────────────────────────
const BLUE   = '#1557FF'
const BLUE_D = '#0d3fc2'
const BLUE_L = '#e8eeff'
const BLACK  = '#111111'
const GREY1  = '#f3f3f3'   // page bg
const GREY2  = '#ffffff'   // card bg
const GREY3  = '#e8e8e8'   // border / divider
const GREY4  = '#a0a0a0'   // muted text
const GREY5  = '#6b6b6b'   // secondary text
const GREEN  = '#16a34a'
const RED    = '#dc2626'
const AMBER  = '#d97706'

const PRIORITY_STYLE = {
  LOW:      { bg: '#dcfce7', color: '#15803d' },
  MEDIUM:   { bg: '#dbeafe', color: '#1d4ed8' },
  HIGH:     { bg: '#ffedd5', color: '#c2410c' },
  CRITICAL: { bg: '#fee2e2', color: '#991b1b' },
}

const STATUS_COLOR = {
  OPEN: GREY4, IN_PROGRESS: BLUE, AUTO_RESOLVED: GREEN,
  ESCALATED: RED, CLOSED: GREY4,
  PENDING: AMBER, CONFIRMED: BLUE, SHIPPED: '#7c3aed',
  DELIVERED: GREEN, CANCELLED: RED,
}

// ── primitives ───────────────────────────────────────────────
const Tag = ({ children, bg, color, style }) => (
  <span style={{
    display: 'inline-block', background: bg, color,
    padding: '2px 8px', borderRadius: 4,
    fontSize: 10, fontWeight: 700, letterSpacing: '.04em',
    whiteSpace: 'nowrap', ...style
  }}>{children}</span>
)

const Pill = ({ on }) => (
  <span style={{
    display: 'inline-flex', alignItems: 'center', gap: 5,
    background: on ? '#dcfce7' : '#fee2e2',
    color: on ? GREEN : RED,
    padding: '3px 9px', borderRadius: 99, fontSize: 10, fontWeight: 700,
  }}>
    <span style={{
      width: 6, height: 6, borderRadius: '50%',
      background: on ? GREEN : RED,
      boxShadow: on ? `0 0 5px ${GREEN}` : 'none',
      display: 'inline-block'
    }} />
    {on ? 'UP' : 'DOWN'}
  </span>
)

const Card = ({ children, style }) => (
  <div style={{
    background: GREY2, borderRadius: 12,
    border: `1px solid ${GREY3}`,
    boxShadow: '0 1px 4px rgba(0,0,0,.05)',
    ...style
  }}>{children}</div>
)

const Label = ({ children }) => (
  <div style={{ fontSize: 11, fontWeight: 600, color: GREY5, marginBottom: 5, letterSpacing: '.03em' }}>
    {children}
  </div>
)

const Input = ({ label, textarea, ...props }) => {
  const base = {
    width: '100%', background: GREY1, border: `1px solid ${GREY3}`,
    color: BLACK, padding: '9px 12px', borderRadius: 8, fontSize: 13,
    outline: 'none', transition: 'border .15s',
  }
  return (
    <div style={{ marginBottom: 12 }}>
      {label && <Label>{label}</Label>}
      {textarea
        ? <textarea style={{ ...base, resize: 'vertical', height: 80 }} {...props} />
        : <input style={base} {...props} />
      }
    </div>
  )
}

const CATEGORIES = ['Electronics','Mobiles','Laptops','Audio','Accessories','Peripherals','Networking','Storage','Wearables','Other']

const Select = ({ label, value, onChange, required }) => {
  const base = {
    width: '100%', background: GREY1, border: `1px solid ${GREY3}`,
    color: value ? BLACK : GREY4, padding: '9px 12px', borderRadius: 8, fontSize: 13,
    outline: 'none', cursor: 'pointer', appearance: 'none',
    backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E")`,
    backgroundRepeat: 'no-repeat', backgroundPosition: 'right 12px center',
  }
  return (
    <div style={{ marginBottom: 12 }}>
      {label && <Label>{label}</Label>}
      <select style={base} value={value} onChange={onChange} required>
        <option value="">Select category…</option>
        {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
      </select>
    </div>
  )
}

const Btn = ({ children, variant = 'primary', small, style, ...props }) => {
  const styles = {
    primary: { background: BLACK, color: '#fff', border: 'none' },
    blue:    { background: BLUE,  color: '#fff', border: 'none' },
    ghost:   { background: 'transparent', color: BLACK, border: `1px solid ${GREY3}` },
    danger:  { background: '#fee2e2', color: RED, border: 'none' },
  }
  return (
    <button style={{
      ...styles[variant],
      padding: small ? '5px 12px' : '9px 20px',
      borderRadius: 8, fontSize: small ? 11 : 13, fontWeight: 600,
      cursor: 'pointer', whiteSpace: 'nowrap',
      transition: 'opacity .15s',
      ...style
    }}
    onMouseEnter={e => e.currentTarget.style.opacity = '.82'}
    onMouseLeave={e => e.currentTarget.style.opacity = '1'}
    {...props}>{children}</button>
  )
}

const Th = ({ children, style }) => (
  <th style={{
    textAlign: 'left', padding: '10px 16px',
    fontSize: 10, fontWeight: 700, color: GREY4,
    letterSpacing: '.08em', textTransform: 'uppercase',
    borderBottom: `1px solid ${GREY3}`, ...style
  }}>{children}</th>
)

const Td = ({ children, style }) => (
  <td style={{
    padding: '11px 16px', fontSize: 13, color: BLACK,
    borderBottom: `1px solid ${GREY1}`,
    verticalAlign: 'middle', ...style
  }}>{children}</td>
)

const SectionLabel = ({ children }) => (
  <div style={{ fontSize: 10, fontWeight: 700, color: GREY4, letterSpacing: '.1em',
    textTransform: 'uppercase', marginBottom: 16 }}>
    {children}
  </div>
)

const Empty = ({ text }) => (
  <div style={{ padding: '48px 0', textAlign: 'center', color: GREY4, fontSize: 13 }}>
    {text}
  </div>
)

// ── data hook ────────────────────────────────────────────────
function useData(url, deps = []) {
  const [data, setData] = useState(null)
  const load = useCallback(async () => {
    try { setData((await axios.get(API_BASE + url)).data) } catch {}
  }, [url])
  useEffect(() => { load() }, [load, ...deps])
  return [data, load]
}

// ── login screen ─────────────────────────────────────────────
function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error,    setError]    = useState('')
  const [loading,  setLoading]  = useState(false)

  const submit = async e => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const res = await axios.post(API_BASE + '/api/auth/login', { username, password })
      sessionStorage.setItem('nexops_token', res.data.token)
      onLogin()
    } catch {
      setError('Invalid username or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh', background: GREY1,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: '"Helvetica Neue", Helvetica, Arial, sans-serif',
    }}>
      <div style={{ width: 360 }}>
        {/* logo */}
        <div style={{ textAlign: 'center', marginBottom: 36 }}>
          <img src="/logo.png" alt="NexOps" style={{ height: 72 }} />
        </div>

        <Card style={{ padding: 32 }}>
          <div style={{ fontWeight: 800, fontSize: 18, marginBottom: 4 }}>Sign in</div>
          <div style={{ fontSize: 12, color: GREY5, marginBottom: 24 }}>Admin access only</div>

          <form onSubmit={submit}>
            <Input
              label="Username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              placeholder="admin"
              autoFocus
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />

            {error && (
              <div style={{ fontSize: 12, color: RED, marginBottom: 12, fontWeight: 600 }}>
                {error}
              </div>
            )}

            <Btn
              type="submit"
              variant="blue"
              style={{ width: '100%', marginTop: 4, opacity: loading ? .6 : 1 }}
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </Btn>
          </form>

          <div style={{
            marginTop: 20, padding: 12, background: GREY1,
            borderRadius: 8, fontSize: 11, color: GREY5,
          }}>
            <span style={{ fontWeight: 700 }}>Demo credentials</span><br />
            Username: <code style={{ color: BLACK }}>admin</code> &nbsp;
            Password: <code style={{ color: BLACK }}>nexops123</code>
          </div>
        </Card>
      </div>
    </div>
  )
}

// ── loading screen ───────────────────────────────────────────
function Loader() {
  return (
    <div style={{
      position: 'fixed', inset: 0, background: GREY1,
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', zIndex: 9999,
    }}>
      <img src="/logo.png" alt="NexOps" style={{ width: 110, marginBottom: 32, opacity: .95 }} />
      <div style={{ display: 'flex', gap: 7 }}>
        {[0,1,2].map(i => (
          <div key={i} style={{
            width: 7, height: 7, borderRadius: '50%', background: BLUE,
            animation: `bounce .9s ease-in-out ${i * .18}s infinite`,
          }} />
        ))}
      </div>
      <style>{`@keyframes bounce{0%,80%,100%{transform:translateY(0)}40%{transform:translateY(-10px)}}`}</style>
    </div>
  )
}

// ══ PANELS ══════════════════════════════════════════════════

// ── Overview ─────────────────────────────────────────────────
function AgentFlowDiagram({ statuses }) {
  const nodes = [
    { key: 'Inventory',  label: 'Inventory',  icon: '📦', color: BLUE,              x: 0   },
    { key: 'Pricing',    label: 'Pricing',    icon: '💹', color: '#111',            x: 340 },
    { key: 'Escalation', label: 'Escalation', icon: '🚨', color: RED,               x: 340 },
    { key: 'Supplier',   label: 'Supplier',   icon: '🏭', color: AMBER,             x: 340 },
  ]
  const up = k => statuses[k] ?? false
  return (
    <Card style={{ padding: '24px 28px', marginBottom: 28, overflow: 'hidden', position: 'relative' }}>
      <div style={{ fontSize: 10, fontWeight: 700, color: GREY4, letterSpacing: '.1em', textTransform: 'uppercase', marginBottom: 20 }}>Event Flow</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 0, position: 'relative' }}>

        {/* Inventory node */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, zIndex: 2 }}>
          <div style={{
            width: 64, height: 64, borderRadius: 16,
            background: up('Inventory') ? BLUE_L : GREY1,
            border: `2px solid ${up('Inventory') ? BLUE : GREY3}`,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2,
            transition: 'all .3s',
          }}>
            <span style={{ fontSize: 22 }}>📦</span>
          </div>
          <span style={{ fontSize: 11, fontWeight: 700, color: up('Inventory') ? BLUE : GREY4 }}>Inventory</span>
          <div style={{ width: 6, height: 6, borderRadius: '50%', background: up('Inventory') ? GREEN : GREY3, boxShadow: up('Inventory') ? `0 0 6px ${GREEN}` : 'none' }} />
        </div>

        {/* Arrow + Kafka bus */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', position: 'relative', padding: '0 16px' }}>
          <div style={{ width: '100%', height: 2, background: `linear-gradient(90deg, ${BLUE}40, #7c3aed40)`, borderRadius: 2, position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: -3, left: 0, width: '30%', height: 8, borderRadius: 4, background: `linear-gradient(90deg, transparent, ${BLUE}80, transparent)`, animation: 'flowPulse 2s linear infinite' }} />
          </div>
          <div style={{ marginTop: 8, background: '#f5f0ff', border: '1px solid #ddd5ff', borderRadius: 8, padding: '4px 12px', fontSize: 10, fontWeight: 700, color: '#7c3aed', letterSpacing: '.04em' }}>
            ⚡ KAFKA
          </div>
          <div style={{ width: '100%', height: 2, background: `linear-gradient(90deg, #7c3aed40, ${AMBER}40)`, borderRadius: 2, marginTop: 8, position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: -3, left: 0, width: '30%', height: 8, borderRadius: 4, background: `linear-gradient(90deg, transparent, #7c3aed80, transparent)`, animation: 'flowPulse 2.4s linear infinite .6s' }} />
          </div>
        </div>

        {/* Right nodes */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, zIndex: 2 }}>
          {[
            { key: 'Pricing',    icon: '💹', color: '#111' },
            { key: 'Escalation', icon: '🚨', color: RED    },
            { key: 'Supplier',   icon: '🏭', color: AMBER  },
          ].map(n => (
            <div key={n.key} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 3, height: 2, background: GREY3, flex: 0 }} />
              <div style={{
                width: 48, height: 48, borderRadius: 12,
                background: up(n.key) ? '#f9fafb' : GREY1,
                border: `2px solid ${up(n.key) ? GREY3 : GREY3}`,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                transition: 'all .3s',
              }}>
                <span style={{ fontSize: 18 }}>{n.icon}</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <span style={{ fontSize: 11, fontWeight: 700, color: BLACK }}>{n.key}</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <div style={{ width: 5, height: 5, borderRadius: '50%', background: up(n.key) ? GREEN : RED, boxShadow: up(n.key) ? `0 0 5px ${GREEN}` : 'none' }} />
                  <span style={{ fontSize: 9, fontWeight: 700, color: up(n.key) ? GREEN : RED }}>{up(n.key) ? 'UP' : 'DOWN'}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <style>{`@keyframes flowPulse{0%{left:-30%}100%{left:110%}}`}</style>
    </Card>
  )
}

function Overview({ products, tickets, orders, pricing, statuses }) {
  const metrics = [
    { label: 'Products in Catalogue', value: products?.length ?? '—', sub: 'inventory items', color: BLACK },
    { label: 'Open Tickets',          value: tickets?.filter(t=>['OPEN','IN_PROGRESS','ESCALATED'].includes(t.status)).length ?? '—', sub: 'need attention', color: RED },
    { label: 'Purchase Orders',       value: orders?.length ?? '—', sub: 'raised by supplier agent', color: AMBER },
    { label: 'Price Adjustments',     value: pricing?.length ?? '—', sub: 'by pricing agent', color: BLUE },
  ]

  const agents = [
    { key: 'Inventory',  icon: '📦', color: BLUE,  desc: 'Watches stock 24/7. Fires events on every sale and low-stock breach.' },
    { key: 'Pricing',    icon: '💹', color: BLACK, desc: 'Raises prices on high demand, cuts them on overstock. No human needed.' },
    { key: 'Escalation', icon: '🚨', color: RED,   desc: 'Reads tickets, uses Gemini AI to triage, assigns to the right team.' },
    { key: 'Supplier',   icon: '🏭', color: AMBER, desc: 'Auto-creates Purchase Orders on low stock. Saga pattern — no duplicates.' },
  ]

  return (
    <div>
      {/* metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12, marginBottom: 28 }}>
        {metrics.map(m => (
          <Card key={m.label} style={{ padding: '20px 22px' }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: GREY5, marginBottom: 10 }}>{m.label}</div>
            <div style={{ fontSize: 38, fontWeight: 800, color: m.color, lineHeight: 1 }}>{m.value}</div>
            <div style={{ fontSize: 11, color: GREY4, marginTop: 6 }}>{m.sub}</div>
          </Card>
        ))}
      </div>

      {/* flow diagram */}
      <AgentFlowDiagram statuses={statuses} />

      {/* agent cards */}
      <SectionLabel>Active AI Agents</SectionLabel>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {agents.map(a => {
          const on = statuses[a.key] ?? false
          return (
            <Card key={a.key} style={{
              padding: '20px 22px',
              borderLeft: `4px solid ${on ? a.color : GREY3}`,
              opacity: on ? 1 : 0.75,
              transition: 'all .3s',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{
                    width: 38, height: 38, borderRadius: 10,
                    background: on ? (a.key === 'Inventory' ? BLUE_L : a.key === 'Pricing' ? '#f3f3f3' : a.key === 'Escalation' ? '#fee2e2' : '#fffbeb') : GREY1,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18,
                  }}>{a.icon}</div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: 14, color: BLACK }}>{a.key} Agent</div>
                    <div style={{ fontSize: 10, color: GREY4, marginTop: 1, fontWeight: 500 }}>AI-driven · autonomous</div>
                  </div>
                </div>
                <Pill on={on} />
              </div>
              <div style={{ fontSize: 12, color: GREY5, lineHeight: 1.7, paddingLeft: 48 }}>{a.desc}</div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}

// ── Inventory ────────────────────────────────────────────────
function EditProductModal({ product, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: product.name,
    category: product.category,
    stockQuantity: product.stockQuantity,
    lowStockThreshold: product.lowStockThreshold,
    basePrice: product.basePrice,
  })
  const set = k => e => setForm(p => ({ ...p, [k]: e.target.value }))

  const save = async e => {
    e.preventDefault()
    await axios.put(API_BASE + `/api/inventory/products/${product.id}`, {
      ...form,
      stockQuantity: +form.stockQuantity,
      lowStockThreshold: +form.lowStockThreshold,
      basePrice: +form.basePrice,
    })
    onSaved()
    onClose()
  }

  return (
    <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,.35)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 }}>
      <Card style={{ width: 360, padding: 24 }}>
        <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 16 }}>Edit Product #{product.id}</div>
        <form onSubmit={save}>
          <Input label="Product Name" value={form.name} onChange={set('name')} required />
          <Select label="Category" value={form.category} onChange={set('category')} required />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <Input label="Stock Qty" type="number" value={form.stockQuantity} onChange={set('stockQuantity')} required />
            <Input label="Low Threshold" type="number" value={form.lowStockThreshold} onChange={set('lowStockThreshold')} required />
          </div>
          <Input label="Base Price (₹)" type="number" value={form.basePrice} onChange={set('basePrice')} required />
          <div style={{ display:'flex', gap:8, marginTop:8 }}>
            <Btn type="submit" variant="blue" style={{ flex:1 }}>Save</Btn>
            <Btn type="button" variant="ghost" style={{ flex:1 }} onClick={onClose}>Cancel</Btn>
          </div>
        </form>
      </Card>
    </div>
  )
}

function InventoryPanel({ onRefresh }) {
  const [products, reload] = useData('/api/inventory/products')
  const [form, setForm] = useState({ name:'', category:'', stockQuantity:'', lowStockThreshold:'', basePrice:'' })
  const [flash, setFlash] = useState('')
  const [editing, setEditing] = useState(null)

  const set = k => e => setForm(p => ({ ...p, [k]: e.target.value }))

  const submit = async e => {
    e.preventDefault()
    try {
      await axios.post(API_BASE + '/api/inventory/products', {
        ...form,
        stockQuantity: +form.stockQuantity,
        lowStockThreshold: +form.lowStockThreshold,
        basePrice: +form.basePrice,
      })
      setFlash('✓ Product added')
      setForm({ name:'', category:'', stockQuantity:'', lowStockThreshold:'', basePrice:'' })
      reload(); onRefresh()
      setTimeout(() => setFlash(''), 3000)
    } catch(e) {
      const msg = e.response?.data?.error || 'Something went wrong'
      setFlash('✗ ' + msg)
      setTimeout(() => setFlash(''), 4000)
    }
  }

  const buy = async id => {
    try { await axios.put(API_BASE+`/api/inventory/products/${id}/stock`,{quantityChange:-1}); reload(); onRefresh() }
    catch(e) { alert(e.message) }
  }

  const del = async (id, name) => {
    if (!window.confirm(`Delete "${name}"? This cannot be undone.`)) return
    try { await axios.delete(API_BASE+`/api/inventory/products/${id}`); reload(); onRefresh() }
    catch { alert('Delete failed') }
  }

  return (
    <>
      {editing && <EditProductModal product={editing} onClose={() => setEditing(null)} onSaved={() => { reload(); onRefresh() }} />}
      <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: 16, alignItems: 'start' }}>
        <Card style={{ padding: 20 }}>
          <SectionLabel>Add Product</SectionLabel>
          <form onSubmit={submit}>
            <Input label="Product Name" value={form.name} onChange={set('name')} required placeholder="e.g. Gaming Laptop" />
            <Select label="Category" value={form.category} onChange={set('category')} required />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <Input label="Stock Qty" type="number" value={form.stockQuantity} onChange={set('stockQuantity')} required />
              <Input label="Low Threshold" type="number" value={form.lowStockThreshold} onChange={set('lowStockThreshold')} required />
            </div>
            <Input label="Base Price (₹)" type="number" value={form.basePrice} onChange={set('basePrice')} required />
            <Btn type="submit" variant="blue" style={{ width: '100%', marginTop: 4 }}>Add Product</Btn>
            {flash && <div style={{ marginTop: 10, fontSize: 12, color: flash.startsWith('✗') ? RED : GREEN, fontWeight: 600 }}>{flash}</div>}
          </form>
        </Card>

        <Card>
          <div style={{ padding: '18px 20px', borderBottom: `1px solid ${GREY3}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 700, fontSize: 14 }}>Products</span>
            <span style={{ fontSize: 12, color: GREY5 }}>{products?.length ?? 0} items</span>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead><tr>
              {['#','Name','Category','Stock','Threshold','Base ₹','Live ₹','Actions'].map(h=><Th key={h}>{h}</Th>)}
            </tr></thead>
            <tbody>
              {(products||[]).map(p=>(
                <tr key={p.id} style={{ transition: 'background .1s' }}
                  onMouseEnter={e=>e.currentTarget.style.background=GREY1}
                  onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                  <Td style={{ color: GREY4 }}>#{p.id}</Td>
                  <Td><b>{p.name}</b></Td>
                  <Td style={{ color: GREY5 }}>{p.category}</Td>
                  <Td>
                    <span style={{ fontWeight: 700, color: p.stockQuantity <= p.lowStockThreshold ? RED : GREEN }}>
                      {p.stockQuantity}
                    </span>
                    {p.stockQuantity <= p.lowStockThreshold &&
                      <Tag bg="#fee2e2" color={RED} style={{ marginLeft: 6 }}>LOW</Tag>}
                  </Td>
                  <Td style={{ color: GREY5 }}>{p.lowStockThreshold}</Td>
                  <Td style={{ color: GREY5 }}>₹{Number(p.basePrice).toLocaleString()}</Td>
                  <Td style={{ color: BLUE, fontWeight: 700 }}>₹{Number(p.currentPrice).toLocaleString()}</Td>
                  <Td>
                    <div style={{ display:'flex', gap:4 }}>
                      <Btn small variant="ghost" onClick={()=>buy(p.id)}>Buy −1</Btn>
                      <Btn small variant="ghost" onClick={()=>setEditing(p)}>Edit</Btn>
                      <Btn small variant="ghost" onClick={()=>del(p.id, p.name)}
                        style={{ color: RED, borderColor: RED }}>Del</Btn>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
          {!products?.length && <Empty text="No products yet — add one on the left" />}
        </Card>
      </div>
    </>
  )
}

// ── Pricing ──────────────────────────────────────────────────
function PricingPanel() {
  const [history] = useData('/api/pricing/history')

  return (
    <Card>
      <div style={{ padding: '18px 20px', borderBottom: `1px solid ${GREY3}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontWeight: 700, fontSize: 14 }}>AI Pricing Decisions</span>
        <span style={{ fontSize: 12, color: GREY5 }}>{history?.length ?? 0} total</span>
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead><tr>
          {['#','Product','Previous Price','New Price','Change','Reason','Time'].map(h=><Th key={h}>{h}</Th>)}
        </tr></thead>
        <tbody>
          {(history||[]).map(r=>{
            const chg = (((r.newPrice-r.previousPrice)/r.previousPrice)*100).toFixed(1)
            const up = chg >= 0
            return (
              <tr key={r.id}
                onMouseEnter={e=>e.currentTarget.style.background=GREY1}
                onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                <Td style={{ color: GREY4 }}>#{r.id}</Td>
                <Td><b>{r.productName}</b></Td>
                <Td style={{ color: GREY5 }}>₹{Number(r.previousPrice).toLocaleString()}</Td>
                <Td style={{ fontWeight: 700 }}>₹{Number(r.newPrice).toLocaleString()}</Td>
                <Td>
                  <Tag bg={up?'#fee2e2':'#dcfce7'} color={up?RED:GREEN}>
                    {up?'▲':'▼'} {Math.abs(chg)}%
                  </Tag>
                </Td>
                <Td style={{ color: GREY5, fontSize: 11 }}>{r.reason}</Td>
                <Td style={{ color: GREY4, fontSize: 11 }}>{r.decidedAt?new Date(r.decidedAt).toLocaleString():'—'}</Td>
              </tr>
            )
          })}
        </tbody>
      </table>
      {!history?.length && <Empty text="Simulate 5 purchases on any product to trigger the Pricing Agent" />}
    </Card>
  )
}

// ── Escalation ───────────────────────────────────────────────
function EscalationPanel({ onRefresh }) {
  const [tickets, reload] = useData('/api/escalation/tickets')
  const [form, setForm] = useState({ customerName:'', customerEmail:'', issueDescription:'' })
  const [flash, setFlash] = useState('')

  const set = k => e => setForm(p=>({...p,[k]:e.target.value}))

  const submit = async e => {
    e.preventDefault()
    try {
      await axios.post(API_BASE+'/api/escalation/tickets', form)
      setFlash('Ticket submitted & triaged')
      setForm({ customerName:'', customerEmail:'', issueDescription:'' })
      reload(); onRefresh()
      setTimeout(()=>setFlash(''), 2500)
    } catch { setFlash('Something went wrong') }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: 16, alignItems: 'start' }}>
      <div>
        <Card style={{ padding: 20, marginBottom: 12 }}>
          <SectionLabel>New Ticket</SectionLabel>
          <form onSubmit={submit}>
            <Input label="Customer Name" value={form.customerName} onChange={set('customerName')} required />
            <Input label="Email" type="email" value={form.customerEmail} onChange={set('customerEmail')} required />
            <Input textarea label="Issue Description" value={form.issueDescription} onChange={set('issueDescription')} required
              placeholder='Try: "urgent refund immediately" or "my order is broken"' />
            <Btn type="submit" variant="blue" style={{ width: '100%', marginTop: 4 }}>Submit Ticket</Btn>
            {flash && <div style={{ marginTop: 10, fontSize: 12, color: GREEN, fontWeight: 600 }}>{flash}</div>}
          </form>
        </Card>

        <Card style={{ padding: 18 }}>
          <SectionLabel>Triage Reference</SectionLabel>
          {[
            ['CRITICAL', RED,   '"urgent", "asap", "immediately", "security"'],
            ['HIGH',     AMBER, '"refund", "broken", "not working"'],
            ['MEDIUM',   BLUE,  '"slow", "issue", "problem"'],
            ['LOW',      GREEN, 'anything else → auto-resolved by AI'],
          ].map(([p,c,ex]) => (
            <div key={p} style={{ display: 'flex', gap: 8, marginBottom: 10, alignItems: 'flex-start' }}>
              <Tag bg={PRIORITY_STYLE[p].bg} color={c} style={{ marginTop: 1, minWidth: 54, textAlign: 'center' }}>{p}</Tag>
              <span style={{ fontSize: 11, color: GREY5, lineHeight: 1.5 }}>{ex}</span>
            </div>
          ))}
        </Card>
      </div>

      <Card>
        <div style={{ padding: '18px 20px', borderBottom: `1px solid ${GREY3}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 700, fontSize: 14 }}>All Tickets</span>
          <span style={{ fontSize: 12, color: GREY5 }}>{tickets?.length ?? 0} total</span>
        </div>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead><tr>
            {['#','Customer','Priority','Status','Assigned To','AI Analysis','Created'].map(h=><Th key={h}>{h}</Th>)}
          </tr></thead>
          <tbody>
            {(tickets||[]).map(t=>{
              const ps = PRIORITY_STYLE[t.priority] || PRIORITY_STYLE.LOW
              return (
                <tr key={t.id}
                  onMouseEnter={e=>e.currentTarget.style.background=GREY1}
                  onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                  <Td style={{ color: GREY4 }}>#{t.id}</Td>
                  <Td>
                    <div style={{ fontWeight: 600 }}>{t.customerName}</div>
                    <div style={{ fontSize: 11, color: GREY4 }}>{t.customerEmail}</div>
                  </Td>
                  <Td><Tag bg={ps.bg} color={ps.color}>{t.priority}</Tag></Td>
                  <Td style={{ color: STATUS_COLOR[t.status]||GREY5, fontWeight: 600, fontSize: 11 }}>{t.status}</Td>
                  <Td style={{ fontSize: 11, color: GREY5 }}>{t.assignedTo}</Td>
                  <Td style={{ fontSize: 11, color: GREY4, maxWidth: 160 }}>{t.aiAnalysis}</Td>
                  <Td style={{ fontSize: 11, color: GREY4 }}>{t.createdAt?new Date(t.createdAt).toLocaleString():'—'}</Td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {!tickets?.length && <Empty text="No tickets yet — submit one on the left" />}
      </Card>
    </div>
  )
}

// ── Supplier ─────────────────────────────────────────────────
function SupplierPanel() {
  const [orders] = useData('/api/supplier/orders')

  return (
    <Card>
      <div style={{ padding: '18px 20px', borderBottom: `1px solid ${GREY3}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontWeight: 700, fontSize: 14 }}>Purchase Orders</span>
        <span style={{ fontSize: 12, color: GREY5 }}>{orders?.length ?? 0} total</span>
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead><tr>
          {['PO #','Product','Supplier','Qty','Est. Cost','Status','Trigger','Expected'].map(h=><Th key={h}>{h}</Th>)}
        </tr></thead>
        <tbody>
          {(orders||[]).map(o=>(
            <tr key={o.id}
              onMouseEnter={e=>e.currentTarget.style.background=GREY1}
              onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
              <Td style={{ fontWeight: 700, color: BLUE }}>PO-{String(o.id).padStart(4,'0')}</Td>
              <Td><b>{o.productName}</b></Td>
              <Td style={{ color: GREY5 }}>{o.supplierName}</Td>
              <Td style={{ fontWeight: 700 }}>{o.quantityOrdered} units</Td>
              <Td>₹{Number(o.estimatedCost).toLocaleString()}</Td>
              <Td>
                <Tag
                  bg={STATUS_COLOR[o.status]+'22'}
                  color={STATUS_COLOR[o.status]||GREY5}
                >{o.status}</Tag>
              </Td>
              <Td style={{ fontSize: 11, color: GREY5 }}>{o.triggeredBy}</Td>
              <Td style={{ fontSize: 11, color: GREY4 }}>{o.expectedDelivery?new Date(o.expectedDelivery).toLocaleDateString():'—'}</Td>
            </tr>
          ))}
        </tbody>
      </table>
      {!orders?.length && <Empty text="No orders yet — drop stock below threshold to trigger the Supplier Agent" />}
    </Card>
  )
}

// ══ ROOT ════════════════════════════════════════════════════
const TABS = [
  { id: 'overview',   label: 'Overview'   },
  { id: 'inventory',  label: 'Inventory'  },
  { id: 'pricing',    label: 'Pricing'    },
  { id: 'escalation', label: 'Escalation' },
  { id: 'supplier',   label: 'Supplier'   },
]

const SERVICES = [
  { name: 'Inventory',  url: '/api/inventory/health'  },
  { name: 'Pricing',    url: '/api/pricing/health'    },
  { name: 'Escalation', url: '/api/escalation/health' },
  { name: 'Supplier',   url: '/api/supplier/health'   },
]

export default function App() {
  const [authed,    setAuthed]    = useState(!!sessionStorage.getItem('nexops_token'))
  const [loading,   setLoading]   = useState(true)
  const [tab,       setTab]       = useState('overview')
  const [statuses,  setStatuses]  = useState({})
  const [tick,      setTick]      = useState(0)

  const [products] = useData('/api/inventory/products',  [tick])
  const [tickets]  = useData('/api/escalation/tickets',  [tick])
  const [orders]   = useData('/api/supplier/orders',     [tick])
  const [pricing]  = useData('/api/pricing/history',     [tick])

  const checkServices = useCallback(async () => {
    const res = {}
    await Promise.all(SERVICES.map(async s => {
      try { await axios.get(API_BASE+s.url,{timeout:3000}); res[s.name]=true }
      catch { res[s.name]=false }
    }))
    setStatuses(res)
  }, [])

  useEffect(() => {
    // brief loading screen
    const t = setTimeout(() => setLoading(false), 1600)
    return () => clearTimeout(t)
  }, [])

  useEffect(() => {
    checkServices()
    const id = setInterval(()=>{ checkServices(); setTick(n=>n+1) }, 10000)
    return () => clearInterval(id)
  }, [checkServices])

  const allUp = Object.keys(statuses).length > 0 && Object.values(statuses).every(Boolean)

  const logout = () => { sessionStorage.removeItem('nexops_token'); setAuthed(false) }

  if (loading) return <Loader />
  if (!authed) return <LoginPage onLogin={() => setAuthed(true)} />

  return (
    <div style={{ minHeight: '100vh', background: GREY1, display: 'flex', flexDirection: 'column' }}>

      {/* ── top nav ── */}
      <header style={{
        background: GREY2, borderBottom: `1px solid ${GREY3}`,
        padding: '0 28px', height: 56,
        display: 'flex', alignItems: 'center', gap: 0,
        position: 'sticky', top: 0, zIndex: 100,
      }}>

        {/* logo only */}
        <div style={{ marginRight: 36, display: 'flex', alignItems: 'center' }}>
          <img src="/logo.png" alt="NexOps" style={{ height: 36, display: 'block' }} />
        </div>

        {/* tabs */}
        <nav style={{ display: 'flex', gap: 2, flex: 1, height: '100%', alignItems: 'center' }}>
          {TABS.map(t => {
            const active = tab === t.id
            return (
              <button key={t.id} onClick={() => setTab(t.id)} style={{
                background: 'none', border: 'none', cursor: 'pointer',
                padding: '0 14px', height: '100%',
                fontSize: 13, fontWeight: active ? 700 : 500,
                color: active ? BLACK : t.dev ? GREY4 : GREY5,
                borderBottom: active ? `2px solid ${BLUE}` : '2px solid transparent',
                transition: 'color .15s, border-color .15s',
                fontFamily: 'inherit',
                display: 'flex', alignItems: 'center', gap: 6,
              }}
              onMouseEnter={e=>{ if(!active) e.currentTarget.style.color = t.dev ? GREY4 : BLACK }}
              onMouseLeave={e=>{ if(!active) e.currentTarget.style.color = t.dev ? GREY4 : GREY5 }}
              >
                {t.label}
                {t.dev && (
                  <span style={{
                    fontSize: 8, fontWeight: 700, letterSpacing: '.05em',
                    background: '#f3f3f3', color: GREY4, border: `1px solid ${GREY3}`,
                    padding: '1px 5px', borderRadius: 4, textTransform: 'uppercase',
                  }}>Dev</span>
                )}
              </button>
            )
          })}
        </nav>

        {/* right: service status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {allUp ? (
            /* all up — just the green pill */
            <Tag bg="#dcfce7" color={GREEN} style={{ fontSize: 11, padding: '4px 10px' }}>
              ● All Systems Up
            </Tag>
          ) : (
            /* show only the DOWN ones */
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {SERVICES.filter(s => !statuses[s.name]).map(s => (
                <div key={s.name} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: RED, display: 'inline-block' }} />
                  <span style={{ fontSize: 11, color: RED, fontWeight: 600 }}>{s.name}</span>
                </div>
              ))}
              <Tag bg="#fee2e2" color={RED} style={{ fontSize: 11, padding: '4px 10px' }}>Degraded</Tag>
            </div>
          )}
          <span style={{ fontSize: 10, color: GREY4 }}>↻ 10s</span>
          <div style={{ width: 1, height: 20, background: GREY3 }} />
          <Btn small variant="ghost" onClick={logout}>Sign out</Btn>
        </div>
      </header>

      {/* ── sub header ── */}
      <div style={{
        background: GREY2, borderBottom: `1px solid ${GREY3}`,
        padding: '12px 28px', display: 'flex', alignItems: 'baseline',
        justifyContent: 'space-between',
      }}>
        <div>
          <span style={{ fontSize: 20, fontWeight: 800, letterSpacing: '-.02em' }}>
            {TABS.find(t=>t.id===tab)?.label}
          </span>
          <span style={{ fontSize: 12, color: GREY5, marginLeft: 12 }}>
            {{ overview:'Platform health & agent status', inventory:'Manage products · simulate purchases',
               pricing:'AI-driven price change log', escalation:'Ticket triage · AI analysis',
               supplier:'Auto-raised purchase orders' }[tab]}
          </span>
        </div>
      </div>

      {/* ── main ── */}
      <main style={{ flex: 1, padding: 24, maxWidth: 1400, minWidth: 900, width: '100%', margin: '0 auto' }}>
        {tab === 'overview'   && <Overview products={products} tickets={tickets} orders={orders} pricing={pricing} statuses={statuses} />}
        {tab === 'inventory'  && <InventoryPanel onRefresh={()=>setTick(n=>n+1)} />}
        {tab === 'pricing'    && <PricingPanel />}
        {tab === 'escalation' && <EscalationPanel onRefresh={()=>setTick(n=>n+1)} />}
        {tab === 'supplier'   && <SupplierPanel />}
      </main>

    </div>
  )
}
