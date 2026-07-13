# Credixa Pro

A modern digital banking platform with real-time transactions, loan management, and administrative dashboard.

![Credixa Pro Banner](credixa-frontend/src/assets/hero.png)

## Features

### Frontend
- **Authentication**: JWT-based auth with protected routes, OTP verification, password reset flow
- **Dashboard**: Real-time balance overview, spending analytics, quick actions
- **Accounts**: Multi-account support (Savings, Current, Fixed Deposit)
- **Transactions**: Deposit, withdrawal, fund transfer with history and filtering
- **Beneficiaries**: Save/pay recurring recipients with verification
- **Loans**: Product browsing, EMI calculator, application tracking
- **Notifications**: Real-time alerts via WebSocket
- **Admin Panel**: User management, transaction monitoring, system stats
- **UI/UX**: Dark mode, skeleton loaders, custom toast notifications, responsive design

### Backend
- **Security**: Spring Security + JWT, role-based access control, 2FA support, account lockout
- **Core**: ACID transactions, fund transfer with rollback, multi-threaded processing
- **Performance**: Redis caching, connection pooling, pagination & sorting
- **Events**: Kafka for notifications, email/SMS simulation
- **API**: Swagger documentation, centralized error handling

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React 19, Vite, Redux Toolkit, Tailwind CSS, Recharts, Framer Motion |
| **Backend** | Spring Boot, MySQL, Redis, Kafka, JWT |
| **Real-time** | STOMP WebSocket, SockJS |
| **Deployment** | Docker-ready |

## Project Structure

```
credixa-bank-application/
├── credixa-frontend/
│   ├── src/
│   │   ├── api/              # Axios instance & endpoints
│   │   ├── components/       # UI components & layouts
│   │   ├── hooks/            # Custom hooks (Auth, WebSocket, Toast)
│   │   ├── pages/            # Route pages (auth, dashboard, loans, etc.)
│   │   ├── store/            # Redux slices & thunks
│   │   ├── utils/            # Helpers & validators
│   │   └── main.jsx          # App entry point
│   ├── public/
│   └── package.json
└── mockup/
    ├── API design.html       # REST API documentation
    ├── SQL script.sql        # Database schema
    └── System design.txt     # Architecture overview
```

## Getting Started

### Prerequisites
- Node.js 20+
- npm or yarn

### Installation

```bash
cd credixa-frontend
npm install
```

### Development

```bash
npm run dev      # Start dev server (Vite)
npm run build    # Production build
npm run lint     # Run ESLint
npm run preview  # Preview production build
```

## UI Screenshots

### Authentication
| Login | Registration | Forgot Password |
|-------|--------------|-----------------|
| ![Login Screen](credixa-frontend/src/assets/screenshots/login.png) | *(Register screenshot pending)* | *(Forgot Password screenshot pending)* |

### Dashboard
![Dashboard Overview](credixa-frontend/src/assets/screenshots/dashboard%201.2.png)

### Accounts & Transactions
| Accounts | Transfer | Transaction History |
|----------|----------|---------------------|
| ![Accounts](credixa-frontend/src/assets/screenshots/account%201.1.png) | ![Transfer](credixa-frontend/src/assets/screenshots/transfer%201.4.png) | ![Transactions](credixa-frontend/src/assets/screenshots/transaction%201.2.png) |

### Loans
![Loans & EMI Calculator](credixa-frontend/src/assets/screenshots/loan%201.3.png)

### Admin Panel
| Main Dashboard | Add User | Branch Management | Loan Management |
|----------------|----------|-----------------|---------------|
| ![Admin Dashboard](credixa-frontend/src/assets/screenshots/admin%201.1.png) | ![Admin Add User](credixa-frontend/src/assets/screenshots/admin%201.1%20add%20user.png) | ![Branch Management](credixa-frontend/src/assets/screenshots/admin%201.3%20branch.png) | ![Loan Management](credixa-frontend/src/assets/screenshots/admin%201.4%20loan%20.png) |

## API Endpoints

Base URL: `/api/v1`

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Authenticate & get JWT |
| POST | `/auth/verify-otp` | Verify email/phone OTP |
| POST | `/auth/forgot-password` | Send password reset OTP |
| POST | `/auth/reset-password` | Reset with OTP |
| POST | `/auth/logout` | Invalidate token |
| PATCH | `/auth/2fa` | Toggle 2FA settings |

### Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accounts` | List user accounts |
| GET | `/accounts/{id}` | Get account details |
| GET | `/accounts/{id}/balance` | Balance inquiry |
| GET | `/accounts/summary` | Dashboard summary |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/transactions` | Transaction history |
| POST | `/transactions/deposit` | Deposit funds |
| POST | `/transactions/withdraw` | Withdraw funds |
| POST | `/transactions/transfer` | Transfer money |
| GET | `/transactions/statement` | Download statement |

### Loans
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/loan-products` | Available loan products |
| GET | `/loans` | User's active loans |
| POST | `/loans/apply` | Apply for loan |
| GET | `/loans/emi-calculator` | EMI calculator |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/users` | List all users |
| PATCH | `/admin/users/{id}/status` | Block/unblock user |
| PATCH | `/admin/users/{id}/kyc` | Update KYC status |
| GET | `/admin/stats` | System statistics |
| GET | `/admin/transactions` | All transactions |

See [API design.html](mockup/API%20design.html) for complete documentation.

## Database Schema

The database schema includes:
- `users` - Customer accounts with KYC status
- `admin_users` - Bank staff with role-based access
- `accounts` - Savings, Current, FD accounts
- `beneficiaries` - Saved transfer recipients
- `transactions` - All financial transactions
- `loan_products` - Available loan types
- `loans` - Customer loan applications
- `notifications` - User alerts

See [SQL script.sql](mockup/SQL%20script.sql) for full schema.

## Environment Variables

Create `.env` in `credixa-frontend/`:

```env
VITE_API_BASE_URL=https://api.credixa.in/api/v1
VITE_WS_URL=wss://api.credixa.in/ws
```

## License

MIT License