import { ApolloClient, InMemoryCache, createHttpLink, split } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';
import API_CONFIG from '../config/apiConfig';

// HTTP link for queries & mutations
const httpLink = createHttpLink({
  uri: `${API_CONFIG.buildBaseHost()}/graphql`,
});

// Auth link — injects JWT from localStorage
const authLink = setContext((_, { headers }) => {
  const token = localStorage.getItem('token');
  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : '',
    },
  };
});

// WebSocket link for subscriptions
const wsLink = new GraphQLWsLink(
  createClient({
    url: `${API_CONFIG.buildBaseHost().replace('http', 'ws')}/graphql`,
    connectionParams: () => {
      const token = localStorage.getItem('token');
      return { Authorization: token ? `Bearer ${token}` : '' };
    },
    shouldRetry: () => true,
  })
);

// Split: subscriptions → WS, everything else → HTTP
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,
  authLink.concat(httpLink)
);

const client = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache({
    typePolicies: {
      Game: { keyFields: ['id'] },
      GameSummary: { keyFields: ['id'] },
      Player: { keyFields: ['id'] },
      User: { keyFields: ['id'] },
    },
  }),
  defaultOptions: {
    watchQuery: { fetchPolicy: 'cache-and-network' },
  },
});

export default client;
