import { useMutation, useQuery } from '@apollo/client/react';
import { GET_ME } from '../graphql/queries';
import { LOGIN, REGISTER, GUEST_LOGIN, ADD_BALANCE } from '../graphql/mutations';

/**
 * Hook that wraps all auth operations with Apollo mutations.
 * Replaces Axios calls in AuthContext.jsx.
 */
export function useAuth() {
  const { data: meData, loading: meLoading, refetch: refetchMe } = useQuery(GET_ME, {
    skip: !localStorage.getItem('token'),
  });

  const [loginMutation] = useMutation(LOGIN);
  const [registerMutation] = useMutation(REGISTER);
  const [guestLoginMutation] = useMutation(GUEST_LOGIN);
  const [addBalanceMutation] = useMutation(ADD_BALANCE);

  const login = async (username, password) => {
    const { data } = await loginMutation({
      variables: { input: { username, password } },
    });
    localStorage.setItem('token', data.login.token);
    await refetchMe();
    return data.login;
  };

  const register = async (name, username, password, email) => {
    const { data } = await registerMutation({
      variables: { input: { name, username, password, email } },
    });
    return data.register;
  };

  const guestLogin = async () => {
    const { data } = await guestLoginMutation();
    localStorage.setItem('token', data.guestLogin.token);
    await refetchMe();
    return data.guestLogin;
  };

  const addBalance = async (amount) => {
    const { data } = await addBalanceMutation({ variables: { amount } });
    return data.addBalance;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  return {
    user: meData?.me || null,
    meLoading,
    refetchMe,
    login,
    register,
    guestLogin,
    addBalance,
    logout,
  };
}
